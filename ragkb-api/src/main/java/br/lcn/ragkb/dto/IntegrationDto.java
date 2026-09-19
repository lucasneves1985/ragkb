package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationActionType;
import br.lcn.ragkb.entity.IntegrationAuthType;
import br.lcn.ragkb.entity.IntegrationType;

public record IntegrationDto(
        String id,
        String name,
        String description,
        String url,
        IntegrationAuthType authType,
        boolean hasCredentials,
        String requestTemplate,
        String outputSchema,
        IntegrationType integrationType,
        String scheduleCron,
        String scheduleTimezone,
        Long scheduleIntervalSeconds,
        String contextDescription,
        String paramsDefinition,
        IntegrationActionType actionType,
        String actionTarget,
        String actionTemplate,
        boolean active,
        String createdBy,
        Integer version,
        Instant createdAt,
        Instant updatedAt) {

    public static IntegrationDto from(Integration i) {
        return new IntegrationDto(
                i.getId(), i.getName(), i.getDescription(), i.getUrl(),
                i.getAuthType(), i.hasCredentials(), i.getRequestTemplate(),
                i.getOutputSchema(), i.getIntegrationType(),
                i.getScheduleCron(), i.getScheduleTimezone(), i.getScheduleIntervalSeconds(),
                i.getContextDescription(), i.getParamsDefinition(),
                i.getActionType(), i.getActionTarget(), i.getActionTemplate(),
                i.isActive(), i.getCreatedBy(), i.getVersion(),
                i.getCreatedAt(), i.getUpdatedAt());
    }
}
