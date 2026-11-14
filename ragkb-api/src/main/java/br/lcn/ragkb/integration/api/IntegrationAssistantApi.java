package br.lcn.ragkb.integration.api;

import java.util.Optional;

public interface IntegrationAssistantApi {
    Optional<IntegrationAssistantResult> handle(String question, String conversationId);
}
