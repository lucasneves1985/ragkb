package br.lcn.ragkb.integration;





import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIntegrationRequest(
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
         * Blank/null mantém a credencial existente.
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
