package br.lcn.ragkb.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationActionType;
import br.lcn.ragkb.entity.IntegrationAuthType;
import br.lcn.ragkb.entity.IntegrationExecution;
import br.lcn.ragkb.repository.IntegrationExecutionRepository;
import br.lcn.ragkb.whatsapp.WhatsAppSendException;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração SCHEDULED: chama a URL com auth e request_template,
 * grava o histórico e dispara a ação configurada (e-mail / WhatsApp). Retry
 * simples: 1 tentativa extra em falha de transporte.
 *
 * Template da ação (action_template) suporta: {{response}} -> body cru da
 * resposta {{response.rates.BRL}} -> campo aninhado do JSON da resposta
 * {{response.items.0.name}} -> índice de array {{today}} / {{now}} -> data/hora
 * ISO-8601 do momento do disparo Token inexistente no JSON é substituído por ""
 * e registrado em warn.
 */
@Slf4j
@Service
public class IntegrationExecutor {

    private static final Logger log = LoggerFactory.getLogger(IntegrationExecutor.class);

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;
    private static final int MESSAGE_MAX_LENGTH = 3000;

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    private static final Pattern ARRAY_INDEX = Pattern.compile("(.+?)\\[(\\d+)]");
    private static final String RESPONSE_ROOT = "response";

    private final IntegrationExecutionRepository executionRepository;
    private final IntegrationCryptoService cryptoService;
    private final WhatsAppService whatsAppService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.ticket.from-email:}")
    private String fromEmail;

    public IntegrationExecutor(IntegrationExecutionRepository executionRepository,
            IntegrationCryptoService cryptoService,
            WhatsAppService whatsAppService,
            JavaMailSender mailSender,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.executionRepository = executionRepository;
        this.cryptoService = cryptoService;
        this.whatsAppService = whatsAppService;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        // Construído UMA vez: defaultHeader no builder injetado acumularia
        // headers entre execuções (o builder é instância única do campo).
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void execute(Integration integration) {
        IntegrationExecution execution = new IntegrationExecution(integration.getId());
        executionRepository.save(execution);

        ResponseEntity<String> response = null;
        Exception lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            execution.setAttempt(attempt);
            try {
                response = call(integration);
                lastError = null;
                break;
            } catch (RestClientException e) {
                lastError = e;
                log.warn("Execução da integração '{}' falhou (tentativa {}/{}): {}",
                        integration.getName(), attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep();
                }
            }
        }

        if (lastError != null) {
            execution.failure(null, null, lastError.getMessage());
            executionRepository.save(execution);
            log.error("Integração '{}' falhou após {} tentativas", integration.getName(), MAX_ATTEMPTS);
            return;
        }

        String body = response.getBody();
        execution.success(response.getStatusCode().value(), body);
        executionRepository.save(execution);

        dispatchAction(integration, body);
    }

    private ResponseEntity<String> call(Integration integration) {
        var requestSpec = restClient.post().uri(integration.getUrl());
        applyAuth(integration, requestSpec);

        String body = integration.getRequestTemplate();
        if (body != null && !body.isBlank()) {
            body = renderBuiltins(body);
            return requestSpec.body(body).retrieve().toEntity(String.class);
        }
        return requestSpec.retrieve().toEntity(String.class);
    }

    private void applyAuth(Integration integration, RestClient.RequestBodySpec requestSpec) {
        if (!integration.hasCredentials()) {
            return;
        }
        String credentials = cryptoService.decrypt(integration.getCredentialsEncrypted());
        switch (integration.getAuthType()) {
            case BEARER ->
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials);
            case BASIC ->
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic "
                        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            case HEADER_CUSTOM ->
                applyCustomHeader(requestSpec, credentials);
            case NONE -> {
                /* sem auth */ }
        }
    }

    private void applyCustomHeader(RestClient.RequestBodySpec requestSpec, String credentialsJson) {
        try {
            JsonNode node = objectMapper.readTree(credentialsJson);
            requestSpec.header(node.get("header").asText(), node.get("value").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Credencial HEADER_CUSTOM inválida na execução.", e);
        }
    }

    /**
     * Placeholders suportados: {{today}} e {{now}} (ISO-8601).
     */
    private String renderBuiltins(String template) {
        var now = Instant.now();
        return template
                .replace("{{today}}", now.toString().substring(0, 10))
                .replace("{{now}}", now.toString());
    }

    private void dispatchAction(Integration integration, String responseBody) {
        if (integration.getActionType() == null
                || integration.getActionType() == IntegrationActionType.NONE) {
            return;
        }
        String message = renderMessage(integration, responseBody);
        try {
            switch (integration.getActionType()) {
                case EMAIL ->
                    sendEmail(integration, message);
                case WHATSAPP ->
                    whatsAppService.sendText(integration.getActionTarget(), message);
                case NONE -> {
                    /* nada a fazer */ }
            }
        } catch (WhatsAppSendException | IllegalStateException e) {
            // A ação falhou mas a integração em si foi executada — log registra o
            // contexto; não marca execução como FAILED retroativamente.
            log.error("Ação '{}' da integração '{}' falhou: {}",
                    integration.getActionType(), integration.getName(), e.getMessage());
        }
    }

    private String renderMessage(Integration integration, String responseBody) {
        String response = responseBody == null ? "" : responseBody;

        if (integration.getActionTemplate() == null || integration.getActionTemplate().isBlank()) {
            return truncateMessage(response);
        }

        // Builtins primeiro ({{today}}/{{now}}), depois extração do JSON da resposta
        String template = renderBuiltins(integration.getActionTemplate());
        JsonNode root = parseOrNull(response);

        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            String value = resolveToken(token, root, response);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return truncateMessage(rendered.toString());
    }

    private JsonNode parseOrNull(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            log.warn("Resposta da integração não é JSON válido; tokens {{response.*}} ficarão vazios.");
            return null;
        }
    }

    private String resolveToken(String token, JsonNode root, String rawResponse) {
        if (RESPONSE_ROOT.equals(token)) {
            return rawResponse;
        }
        if (!token.startsWith(RESPONSE_ROOT + ".")) {
            log.warn("Token '{{{{{}}}}}' desconhecido no action_template — será substituído por vazio.", token);
            return "";
        }
        JsonNode node = navigate(root, token.substring(RESPONSE_ROOT.length() + 1));
        if (node == null || node.isMissingNode() || node.isNull()) {
            log.warn("Token '{{{{{}}}}}' não encontrado no JSON da resposta — será substituído por vazio.", token);
            return "";
        }
        // Textual via asText() (sem aspas); objetos/arrays via toString()
        return node.isValueNode() ? node.asText() : node.toString();
    }

    /**
     * Navega por caminho pontilhado, com suporte a índice de array (items[0] ou
     * items.0).
     */
    private JsonNode navigate(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode node = root;
        for (String part : path.split("\\.")) {
            if (node == null) {
                return null;
            }
            Matcher index = ARRAY_INDEX.matcher(part);
            if (index.matches()) {
                node = node.get(index.group(1));
                if (node != null && node.isArray()) {
                    node = node.get(Integer.parseInt(index.group(2)));
                }
            } else if (node.isArray() && part.matches("\\d+")) {
                node = node.get(Integer.parseInt(part));
            } else {
                node = node.get(part);
            }

        }
        return node;
    }

    private String truncateMessage(String value) {
        if (value == null || value.length() <= MESSAGE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, MESSAGE_MAX_LENGTH) + "…";
    }

    private void sendEmail(Integration integration, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setRecipients(Message.RecipientType.TO, integration.getActionTarget());
            message.setSubject("[ragkb] Integração: " + integration.getName());
            message.setText(body, "UTF-8");
            mailSender.send(message);
            log.info("Ação EMAIL da integração '{}' enviada para {}", integration.getName(),
                    integration.getActionTarget());
        } catch (MessagingException e) {
            throw new IllegalStateException("Falha ao enviar e-mail da integração "
                    + integration.getName(), e);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
