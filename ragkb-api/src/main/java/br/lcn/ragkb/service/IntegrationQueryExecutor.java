package br.lcn.ragkb.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.Integration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração QUERY confirmada pelo roteamento (frente 5). Fase 2:
 * auth/template/truncamento delegados ao IntegrationHttpClient (eram duplicados
 * daqui com o IntegrationExecutor).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationQueryExecutor {

    private static final int RESPONSE_MAX_LENGTH = 4000;

    private final IntegrationHttpClient httpClient;

    public record QueryExecutionResult(boolean success, String content) {

    }

    public QueryExecutionResult execute(Integration integration, String question,
            Map<String, String> params) {
        try {
            ResponseEntity<String> response = httpClient.call(integration, question, params);
            String body = response.getBody() == null ? "" : response.getBody();
            if (body.length() > RESPONSE_MAX_LENGTH) {
                body = body.substring(0, RESPONSE_MAX_LENGTH) + "…";
            }
            return new QueryExecutionResult(true,
                    "Resultado da integração '" + integration.getName() + "':\n" + body);
        } catch (Exception e) {
            log.warn("[routing] Integração '{}' falhou na execução: {}",
                    integration.getName(), e.getMessage());
            return new QueryExecutionResult(false, e.getMessage());
        }
    }
}
