package br.lcn.ragkb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationActionType;
import br.lcn.ragkb.entity.IntegrationExecution;
import br.lcn.ragkb.metrics.RoutingMetrics;
import br.lcn.ragkb.repository.IntegrationExecutionRepository;
import br.lcn.ragkb.whatsapp.WhatsAppSendException;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração SCHEDULED: chama a URL, grava o histórico e dispara a
 * ação configurada (e-mail / WhatsApp). Retry simples: 1 tentativa extra em
 * falha de transporte.
 *
 * P4 — consolidação: a chamada HTTP (método GET/POST, auth, placeholders na URL
 * e no body, parsing leniente) e a renderização da mensagem (action_template,
 * listas {{response.items.*}}, teto, lista vazia) são DELEGADAS ao
 * IntegrationHttpClient — eram cópias divergentes deste arquivo. O executor
 * retém apenas o que é exclusivo do fluxo agendado: retry, persistência do
 * histórico (IntegrationExecution) e despacho das ações.
 *
 * Comportamentos preservados deliberadamente: - truncamento da MENSAGEM da ação
 * em 3000 caracteres (MESSAGE_MAX_LENGTH, mais conservador que o truncamento de
 * renderização do cliente); - a ação falhando NÃO marca a execução como FAILED
 * retroativamente; - RestClientResponseException entra no retry (comportamento
 * antigo de retrieve() — 4xx/5xx são "falha de transporte" para o retry).
 *
 * Mudanças de comportamento que a delegação traz (melhorias conscientes): -
 * integrações SCHEDULED agora aceitam GET (httpMethod é respeitado); -
 * {{today}}/{{now}} passam a funcionar também na URL; - parsing leniente
 * (vírgula final/aspas simples) também aqui; - telemetria (RoutingMetrics)
 * cobre execuções agendadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationExecutor {

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2000;
    private static final int MESSAGE_MAX_LENGTH = 3000;

    private final IntegrationExecutionRepository executionRepository;
    private final IntegrationHttpClient httpClient;
    private final WhatsAppService whatsAppService;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final RoutingMetrics metrics;

    @Value("${app.ticket.from-email:}")
    private String fromEmail;

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
                metrics.executionFailed(integration.getName());
                metrics.executionDuration(integration.getName(),
                        integration.getHttpMethod().name(), 0);
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
        metrics.executionSuccess(integration.getName());
        metrics.executionDuration(integration.getName(),
                integration.getHttpMethod().name(), 0);
        execution.success(response.getStatusCode().value(), body);
        executionRepository.save(execution);
        dispatchAction(integration, body);
    }

    /**
     * Delega ao cliente consolidado: question=null e params vazios — a URL
     * renderiza apenas os builtins ({{today}}/{{now}}); placeholders
     * {{param}}/{{question}} em integração SCHEDULED viram vazio com warn (não
     * fazem sentido em fluxo agendado — revise o cadastro da integração se o
     * warn aparecer).
     */
    private ResponseEntity<String> call(Integration integration) {
        return httpClient.call(integration, null, java.util.Map.of());
    }

    private void dispatchAction(Integration integration, String responseBody) {
        if (integration.getActionType() == null
                || integration.getActionType() == IntegrationActionType.NONE) {
            return;
        }
        String message = truncateMessage(
                httpClient.renderResponseMessage(integration, responseBody));
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

    /**
     * Teto de mensagem das ações — mais conservador que o truncamento de
     * renderização do cliente (4000): WhatsApp/e-mail têm limites próprios.
     */
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
