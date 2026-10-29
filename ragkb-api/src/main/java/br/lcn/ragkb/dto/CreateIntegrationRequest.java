package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.IntegrationActionType;
import br.lcn.ragkb.entity.IntegrationAuthType;
import br.lcn.ragkb.entity.IntegrationHttpMethod;
import br.lcn.ragkb.entity.IntegrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIntegrationRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        String description,
        @NotBlank
        @Size(max = 500)
        String url,
        @NotNull
        IntegrationHttpMethod httpMethod,
        @NotNull
        IntegrationAuthType authType,
        /**
         * Enviada apenas em create/update; nunca retornada.
         */
        String credentials,
        String requestTemplate,
        String outputSchema,
        @NotNull
        IntegrationType integrationType,
        String scheduleCron,
        String scheduleTimezone,
        Long scheduleIntervalSeconds,
        String contextDescription,
        String paramsDefinition,
        @NotNull
        IntegrationActionType actionType,
        String actionTarget,
        String actionTemplate,
        boolean active
        ) {

}
