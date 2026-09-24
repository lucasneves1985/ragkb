package br.lcn.ragkb.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.Integration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração QUERY confirmada pelo roteamento (frente 5).
 * Auth/template/truncamento delegados ao IntegrationHttpClient. Fase 2b: quando
 * a integração tem action_template, o body da resposta é renderizado por ele
 * ({{response.campo}}) — o usuário vê a mensagem formatada no chat em vez do
 * JSON cru. Sem template, mantém o body cru (comportamento atual).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationQueryExecutor {

    private final IntegrationHttpClient httpClient;

    public record QueryExecutionResult(boolean success, String content) {

    }

    public QueryExecutionResult execute(Integration integration, String question,
            Map<String, String> params) {
        try {
            var response = httpClient.call(integration, question, params);
            String body = response.getBody() == null ? "" : response.getBody();
            // Com action_template: mensagem formatada; sem: body cru truncado
            String content = httpClient.renderResponseMessage(integration, body);
            return new QueryExecutionResult(true,
                    "Resultado da integração '" + integration.getName() + "':\n" + content);
        } catch (Exception e) {
            log.warn("[routing] Integração '{}' falhou na execução: {}",
                    integration.getName(), e.getMessage());
            return new QueryExecutionResult(false, e.getMessage());
        }
    }
}
