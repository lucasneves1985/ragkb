package br.lcn.ragkb.integration;



import java.time.Instant;

public record IntegrationExecutionDto(
        String id,
        String integrationId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Integer attempt,
        Integer httpStatus,
        String responseBody,
        String errorMessage,
        String actionStatus,
        String actionError) {

    public static IntegrationExecutionDto from(IntegrationExecution e) {
        return new IntegrationExecutionDto(
                e.getId(), e.getIntegrationId(), e.getStatus().name(),
                e.getStartedAt(), e.getFinishedAt(), e.getAttempt(),
                e.getHttpStatus(), e.getResponseBody(), e.getErrorMessage(),
                e.getActionStatus(), e.getActionError());
    }
}
