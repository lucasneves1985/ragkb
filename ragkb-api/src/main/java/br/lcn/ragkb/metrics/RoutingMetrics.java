package br.lcn.ragkb.metrics;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Telemetria do roteamento em camadas e integrações (frente 5, P3 do backlog).
 * Encapsula o MeterRegistry — nenhum service conhece Micrometer diretamente.
 *
 * Convenções: - Tag "integration" usa o NOME da integração (único, garantido
 * pelo DuplicateIntegrationNameException) — legível nos dashboards, enquanto o
 * id é opaco. Cardinalidade controlada: conjunto pequeno e fechado. - PROIBIDO
 * tag com valor de pergunta/usuário/parâmetro — além do custo de cardinalidade,
 * dados de saúde não viram label de métrica.
 *
 * Funis diagnósticos: - Qualidade do gate: verification(vetoed) /
 * (confirmed+vetoed) - Calibração do piso: distribuição de gate.score ×
 * desfecho da verificação - Atrito da coleta: (posteriormente) flow.completed
 * vs flow.expired - Saúde das APIs: execution(failed) + parse(outcome=lenient)
 */
@Component
public class RoutingMetrics {

    private static final String GATE = "ragkb.routing.gate";
    private static final String GATE_SCORE = "ragkb.routing.gate.score";
    private static final String VERIFY = "ragkb.routing.verification";
    private static final String EXEC = "ragkb.integration.execution";
    private static final String EXEC_DURATION = "ragkb.integration.http.duration";
    private static final String PARSE = "ragkb.integration.parse";

    private final MeterRegistry registry;

    public RoutingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Gate vetorial: nenhuma candidata acima do piso (ou sem embeddings).
     */
    public void gateNoCandidate() {
        registry.counter(GATE, "outcome", "no_candidate").increment();
    }

    /**
     * Gate vetorial: ao menos uma candidata acima do piso — segue à
     * verificação.
     */
    public void gateCandidate() {
        registry.counter(GATE, "outcome", "candidate").increment();
    }

    /**
     * Score de similaridade por integração — insumo da recalibração do piso.
     */
    public void gateScore(String integration, double similarity) {
        registry.summary(GATE_SCORE, "integration", integration).record(similarity);
    }

    /**
     * Verificação LLM confirmou a candidata (gate estava certo).
     */
    public void verificationConfirmed(String integration) {
        registry.counter(VERIFY, "decision", "confirmed", "integration", integration).increment();
    }

    /**
     * Verificação LLM vetou a candidata — falso positivo do gate.
     */
    public void verificationVetoed() {
        registry.counter(VERIFY, "decision", "vetoed").increment();
    }

    /**
     * Verificação LLM falhou (sem resposta/exceção) — fail-safe para KB.
     */
    public void verificationError() {
        registry.counter(VERIFY, "decision", "error").increment();
    }

    public void executionSuccess(String integration) {
        registry.counter(EXEC, "integration", integration, "outcome", "success").increment();
    }

    public void executionFailed(String integration) {
        registry.counter(EXEC, "integration", integration, "outcome", "failed").increment();
    }

    /**
     * Latência da chamada HTTP por integração/método — base para timeout
     * futuro.
     */
    public void executionDuration(String integration, String method, long millis) {
        registry.timer(EXEC_DURATION, "integration", integration, "method", method)
                .record(Duration.ofMillis(millis));
    }

    /**
     * Parse do body: strict (Jackson padrão), lenient (fallback — JSON com
     * desvios, ex.: vírgula final), invalid (nem leniente parseou).
     */
    public void parseOutcome(String integration, String outcome) {
        registry.counter(PARSE, "integration", integration, "outcome", outcome).increment();
    }
}
