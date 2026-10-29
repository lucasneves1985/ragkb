package br.lcn.ragkb.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.metrics.RoutingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração QUERY confirmada pelo roteamento (frente 5). Fase 2:
 * auth/template/truncamento delegados ao IntegrationHttpClient. P3: contadores
 * de execução (sucesso/falha) e timer de latência por integração.
 *
 * FIX de wiring: até esta versão o body cru ia direto para o chat — o
 * renderResponseMessage (templates {{response.*.campo}}, listas, teto e lista
 * vazia) existia no IntegrationHttpClient mas NUNCA era chamado neste fluxo.
 * Agora: com action_template, o template manda (cabeçalho é responsabilidade do
 * template); sem template, mantém o formato antigo ("Resultado da integração
 * 'X':\n" + body cru truncado) — fallback legado preservado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationQueryExecutor {

    private final IntegrationHttpClient httpClient;
    private final RoutingMetrics metrics;

    public record QueryExecutionResult(boolean success, String content) {

    }

    public QueryExecutionResult execute(Integration integration, String question,
            Map<String, String> params) {
        long startNanos = System.nanoTime();
        try {
            ResponseEntity<String> response = httpClient.call(integration, question, params);
            String body = response.getBody() == null ? "" : response.getBody();
            metrics.executionSuccess(integration.getName());
            metrics.executionDuration(integration.getName(),
                    integration.getHttpMethod().name(), elapsedMillis(startNanos));

            // O renderResponseMessage cuida de: template único, bloco de itens
            // de lista (com teto e aviso), lista vazia (mensagem honesta) e
            // truncamento — inclusive do body cru no fallback.
            String rendered = httpClient.renderResponseMessage(integration, body);
            boolean hasTemplate = integration.getActionTemplate() != null
                    && !integration.getActionTemplate().isBlank();
            String content = hasTemplate
                    ? rendered
                    : "Resultado da integração '" + integration.getName() + "':\n" + rendered;
            return new QueryExecutionResult(true, content);
        } catch (Exception e) {
            metrics.executionFailed(integration.getName());
            metrics.executionDuration(integration.getName(),
                    integration.getHttpMethod().name(), elapsedMillis(startNanos));
            log.warn("[routing] Integração '{}' falhou na execução: {}",
                    integration.getName(), e.getMessage());
            return new QueryExecutionResult(false, e.getMessage());
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
