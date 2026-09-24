package br.lcn.ragkb.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

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
 *
 * Fase 2b: a renderização da mensagem (action_template) foi consolidada no
 * IntegrationHttpClient — esta classe delega.
 */
@Service
public class IntegrationExecutor {

    private static final Logger log = LoggerFactory.getLogger(IntegrationExecutor.class);
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;

    private final IntegrationExecutionRepository executionRepository;
    private final IntegrationCryptoService cryptoService;
    private final WhatsAppService whatsAppService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final IntegrationHttpClient httpClient;
    private final RestClient restClient;

    @Value("${app.ticket.from-email:}")
    private String fromEmail;

    public IntegrationExecutor(IntegrationExecutionRepository executionRepository,
            IntegrationCryptoService cryptoService,
            WhatsAppService whatsAppService,
            JavaMailSender mailSender,
            ObjectMapper objectMapper,
            IntegrationHttpClient httpClient,
            RestClient.Builder restClientBuilder) {
        this.executionRepository = executionRepository;
        this.cryptoService = cryptoService;
        this.whatsAppService = whatsAppService;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
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
        String message = httpClient.renderResponseMessage(integration, responseBody);
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
