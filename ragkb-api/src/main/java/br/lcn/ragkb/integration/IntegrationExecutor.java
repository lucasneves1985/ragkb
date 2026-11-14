package br.lcn.ragkb.integration;

import br.lcn.ragkb.integration.http.IntegrationHttpClient;
import br.lcn.ragkb.shared.mail.EmailDeliveryException;
import br.lcn.ragkb.shared.mail.EmailService;
import br.lcn.ragkb.shared.metrics.RoutingMetrics;
import br.lcn.ragkb.whatsapp.api.WhatsAppMessagingApi;
import br.lcn.ragkb.whatsapp.api.WhatsAppSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executa uma integração SCHEDULED: chama a URL, grava o histórico e dispara a
 * ação configurada (e-mail / WhatsApp). Retry simples: 1 tentativa extra em
 * falha de transporte.
 *
 * A chamada HTTP e a renderização da mensagem são delegadas ao
 * IntegrationHttpClient. Quando o HTTP funciona, o status principal da
 * execução permanece SUCCESS; eventuais falhas da ação são registradas em
 * action_status/action_error, sem sobrescrever o resultado HTTP.
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
    private final WhatsAppMessagingApi whatsAppService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final RoutingMetrics metrics;

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

        String actionError = dispatchAction(integration, body);
        if (actionError != null) {
            execution.actionFailure(actionError);
        } else if (integration.getActionType() != null
                && integration.getActionType() != IntegrationActionType.NONE) {
            execution.actionSuccess();
        }
        executionRepository.save(execution);
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

    private String dispatchAction(Integration integration, String responseBody) {
        if (integration.getActionType() == null
                || integration.getActionType() == IntegrationActionType.NONE) {
            return null;
        }
        String message = truncateMessage(
                httpClient.renderResponseMessage(integration, responseBody));
        try {
            switch (integration.getActionType()) {
                case EMAIL -> sendEmail(integration, message);
                case WHATSAPP -> whatsAppService.sendText(integration.getActionTarget(), message);
                case NONE -> { /* nada a fazer */ }
            }
            return null;
        } catch (EmailDeliveryException e) {
            log.error("Ação EMAIL da integração '{}' falhou: {}", integration.getName(), e.getMessage());
            return e.getMessage();
        } catch (WhatsAppSendException e) {
            log.error("Ação WHATSAPP da integração '{}' falhou: {}", integration.getName(), e.getMessage());
            return e.getMessage();
        } catch (RuntimeException e) {
            log.error("Ação '{}' da integração '{}' falhou: {}",
                    integration.getActionType(), integration.getName(), e.getMessage(), e);
            return e.getMessage();
        }
    }

    private void sendEmail(Integration integration, String body) {
        emailService.send(integration.getActionTarget(),
                "[ragkb] Integração: " + integration.getName(), body);
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

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
