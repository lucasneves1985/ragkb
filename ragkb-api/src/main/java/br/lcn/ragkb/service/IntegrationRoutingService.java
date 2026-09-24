package br.lcn.ragkb.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.IntegrationType;
import br.lcn.ragkb.repository.IntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gate de roteamento em camadas (frente 5, desenho revisado): 1. Gate vetorial
 * determinístico — similaridade pergunta × descrição da integração QUERY. Sem
 * candidato acima do piso → fluxo KB normal. 2. Verificação de intenção via LLM
 * (structured output) APENAS sobre os candidatos — o modelo tem poder de VETO,
 * não de entusiasmo. 3. Confirmado → execução direta da integração (sem LLM de
 * resposta).
 *
 * Fail-safe: qualquer falha na verificação rejeita → KB flow (o caminho
 * arriscado é executar a integração; rejeitar só custa uma resposta pior). Toda
 * decisão é logada com scores — base para calibrar o threshold.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationRoutingService {

    private static final int MAX_CANDIDATES = 3;

    private static final String VERIFY_SYSTEM_PROMPT = """
            Você é um roteador de intenções de um assistente corporativo.
            Você recebe a pergunta do usuário e uma lista de integrações candidatas
            (cada uma descreve EXATAMENTE que dados operacionais retorna).
            Decida se a pergunta pede os dados operacionais em TEMPO REAL que alguma
            integração retorna.
            REGRAS:
            1. Perguntas de CONHECIMENTO (como fazer, política, procedimento, conceito)
               devem ser REJEITADAS mesmo se mencionarem o mesmo assunto da integração.
            2. Em caso de dúvida entre conhecimento e dado operacional, REJEITE.
            3. Se nenhuma integração retornar exatamente o que a pergunta pede, REJEITE.
            4. Se aceitar, o campo integracao deve ser o nome EXATO da integração escolhida.
            Responda com: executar (boolean), integracao (nome exato ou null), motivo (curto).
            """;

    /**
     * Decisão estruturada do LLM (mesmo padrão do ReminderExtractionService).
     */
    public record RoutingDecision(Boolean executar, String integracao, String motivo) {

    }

    /**
     * Resultado confirmado do roteamento: qual integração executar e por quê.
     */
    public record RoutingOutcome(String integrationId, String integrationName,
            String contextDescription, double similarity, String motivo) {

    }

    private final IntegrationRepository integrationRepository;
    private final IntegrationEmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatClient chatClient;

    @Value("${app.routing.enabled:false}")
    private boolean enabled;

    @Value("${app.routing.similarity-threshold:0.80}")
    private double similarityThreshold;

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return presente somente quando o roteamento CONFIRMOU uma integração;
     * vazio = seguir fluxo normal da KB (sem candidato, LLM rejeitou ou falhou
     * na verificação).
     */
    public Optional<RoutingOutcome> decide(String question) {
        if (!enabled) {
            return Optional.empty();
        }
        var queryIntegrations = integrationRepository
                .findByIntegrationTypeAndActiveTrue(IntegrationType.QUERY);
        if (queryIntegrations.isEmpty()) {
            return Optional.empty();
        }

        float[] questionEmbedding = embeddingModel.embed(question);
        List<IntegrationEmbeddingStore.CandidateMatch> top
                = embeddingStore.findTop(questionEmbedding, MAX_CANDIDATES);

        // Telemetria: melhor score SEMPRE no log, mesmo rejeitando — é a base
        // da calibração do piso (sem isso, falso negativo é invisível).
        // String.format ANTES do log.info: %.3f não é placeholder do SLF4J
        // (que só substitui {}) — passado direto, os argumentos deslocavam.
        if (top.isEmpty()) {
            log.info("[routing] Nenhuma integração QUERY com embedding — fluxo KB normal. Pergunta: '{}'",
                    question);
            return Optional.empty();
        }
        log.info(String.format("[routing] Top similaridades para '%s': %s", question,
                top.stream()
                        .map(c -> String.format("%s=%.3f", c.name(), c.similarity()))
                        .collect(Collectors.joining(", "))),
                question);

        List<IntegrationEmbeddingStore.CandidateMatch> candidates = top.stream()
                .filter(c -> c.similarity() >= similarityThreshold)
                .toList();
        if (candidates.isEmpty()) {
            log.info(String.format(
                    "[routing] Melhor score %.3f abaixo do piso %s — fluxo KB normal. Pergunta: '%s'",
                    top.get(0).similarity(), similarityThreshold, question));
            return Optional.empty();
        }

        RoutingDecision decision = verifyWithLlm(question, candidates);
        log.info(String.format("[routing] Decisão do LLM: executar=%s, integracao=%s, motivo=%s",
                decision.executar(), decision.integracao(), decision.motivo()));

        if (!Boolean.TRUE.equals(decision.executar())) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(c -> c.name().equals(decision.integracao()))
                .findFirst()
                .map(c -> new RoutingOutcome(c.id(), c.name(), c.contextDescription(),
                c.similarity(), decision.motivo()));
    }

    private RoutingDecision verifyWithLlm(String question,
            List<IntegrationEmbeddingStore.CandidateMatch> candidates) {
        String candidatesBlock = candidates.stream()
                .map(c -> String.format("- nome: %s | retorna: %s | similaridade: %.3f",
                c.name(), c.contextDescription(), c.similarity()))
                .collect(Collectors.joining("\n"));
        String userPrompt = """
                Pergunta do usuário: %s

                Integrações candidatas:
                %s
                """.formatted(question, candidatesBlock);

        try {
            RoutingDecision decision = chatClient.prompt()
                    .system(VERIFY_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .entity(RoutingDecision.class);
            if (decision == null) {
                return new RoutingDecision(false, null, "verificação sem resposta");
            }
            // Aceitou mas citou nome inexistente → trata como rejeição
            boolean known = decision.integracao() != null && candidates.stream()
                    .anyMatch(c -> c.name().equals(decision.integracao()));
            if (Boolean.TRUE.equals(decision.executar()) && !known) {
                log.warn(String.format(
                        "[routing] LLM aceitou mas citou integração inexistente '%s' — rejeitando",
                        decision.integracao()));
                return new RoutingDecision(false, decision.integracao(),
                        "nome de integração não confere com candidatos");
            }
            return decision;
        } catch (Exception e) {
            log.error(String.format(
                    "[routing] Falha na verificação LLM — rejeitando (fail-safe para KB): %s",
                    e.getMessage()));
            return new RoutingDecision(false, null, "falha na verificação: " + e.getMessage());
        }
    }
}
