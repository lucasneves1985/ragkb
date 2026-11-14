package br.lcn.ragkb.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.integration.api.IntegrationAssistantApi;
import br.lcn.ragkb.integration.api.IntegrationAssistantResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Integration-owned routing, parameter collection and execution flow. */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationAssistantService implements IntegrationAssistantApi {

    private final IntegrationRoutingService routingService;
    private final IntegrationRepository integrationRepository;
    private final IntegrationParamService paramService;
    private final RedisIntegrationParamStateService paramStateService;
    private final IntegrationQueryExecutor queryExecutor;

    @Override
    public Optional<IntegrationAssistantResult> handle(String question, String conversationId) {
        if (!routingService.isEnabled()) {
            return Optional.empty();
        }

        var pendingOpt = paramStateService.get(conversationId);
        if (pendingOpt.isPresent()) {
            var pendingIntegration = integrationRepository.findById(pendingOpt.get().integrationId());
            if (pendingIntegration.isPresent() && pendingIntegration.get().isActive()) {
                return Optional.of(handlePendingParams(pendingIntegration.get(), pendingOpt.get(),
                        question, conversationId));
            }
            // Integração sumiu/desativou — estado órfão, segue fluxo normal
            paramStateService.clear(conversationId);
        }

        var outcome = routingService.decide(question);
        if (outcome.isPresent()) {
            var integrationOpt = integrationRepository.findById(outcome.get().integrationId());
            if (integrationOpt.isPresent()) {
                return Optional.of(handleIntegrationQuery(integrationOpt.get(), question, conversationId));
            }
            log.warn("[routing] Integração '{}' confirmada mas não encontrada — fluxo KB normal",
                    outcome.get().integrationName());
        }
        return Optional.empty();
    }

    private IntegrationAssistantResult handleIntegrationQuery(
            Integration integration, String question, String conversationId) {
        Map<String, String> params = Map.of();
        if (paramService.hasRequiredParams(integration)) {
            var extracted = paramService.extract(integration, question);
            var missing = paramService.findMissingRequired(integration, extracted);
            if (!missing.isEmpty()) {
                paramStateService.save(conversationId, integration.getId(), extracted);
                String ask = paramService.buildAskMessage(integration, missing);
                return new IntegrationAssistantResult(
                        IntegrationAssistantResult.Kind.PARAM_REQUIRED,
                        integration.getName(), ask, ask);
            }
            params = extracted;
        }
        return execute(integration, params, question, conversationId);
    }

    private IntegrationAssistantResult handlePendingParams(
            Integration integration, RedisIntegrationParamStateService.PendingParamState pending,
            String question, String conversationId) {
        var extracted = paramService.extract(integration, question);
        Map<String, String> merged = new HashMap<>(pending.params());
        merged.putAll(extracted);
        var missing = paramService.findMissingRequired(integration, merged);
        if (!missing.isEmpty()) {
            paramStateService.save(conversationId, integration.getId(), merged);
            String ask = paramService.buildAskMessage(integration, missing);
            return new IntegrationAssistantResult(
                    IntegrationAssistantResult.Kind.PARAM_REQUIRED,
                    integration.getName(), ask, ask);
        }
        paramStateService.clear(conversationId);
        return execute(integration, merged, question, conversationId);
    }

    private IntegrationAssistantResult execute(Integration integration,
            Map<String, String> params, String question, String conversationId) {
        var execution = queryExecutor.execute(integration, question, params);
        if (execution.success()) {
            return new IntegrationAssistantResult(
                    IntegrationAssistantResult.Kind.RESULT,
                    integration.getName(), execution.content(), question);
        }
        paramStateService.clear(conversationId);
        return new IntegrationAssistantResult(
                IntegrationAssistantResult.Kind.FAILED,
                integration.getName(), execution.content(), question);
    }
}
