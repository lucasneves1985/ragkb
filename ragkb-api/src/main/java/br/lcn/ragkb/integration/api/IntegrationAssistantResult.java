package br.lcn.ragkb.integration.api;

public record IntegrationAssistantResult(
        Kind kind,
        String integrationName,
        String answer,
        String interactionQuestion) {

    public enum Kind {
        PARAM_REQUIRED,
        RESULT,
        FAILED
    }
}
