package br.lcn.ragkb.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.entity.Integration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Executa uma integração QUERY confirmada pelo roteamento (frente 5).
 * Duplicação deliberada do applyAuth/renderTemplate com o IntegrationExecutor
 * (agendadas) — refactor para cliente HTTP compartilhado na fase 2, quando a
 * arquitetura estiver validada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationQueryExecutor {

    private static final int RESPONSE_MAX_LENGTH = 4000;

    private final IntegrationCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public record QueryExecutionResult(boolean success, String content) {

    }

    public QueryExecutionResult execute(Integration integration, String question) {
        try {
            var requestSpec = restClientBuilder.build()
                    .post()
                    .uri(integration.getUrl());
            applyAuth(integration, requestSpec);

            String body = integration.getRequestTemplate();
            org.springframework.http.ResponseEntity<String> response;
            if (body != null && !body.isBlank()) {
                response = requestSpec.body(renderTemplate(body, question))
                        .retrieve()
                        .toEntity(String.class);
            } else {
                response = requestSpec.retrieve().toEntity(String.class);
            }
            return new QueryExecutionResult(true, formatResult(integration, response.getBody()));
        } catch (Exception e) {
            log.warn("[routing] Integração '{}' falhou na execução: {}",
                    integration.getName(), e.getMessage());
            return new QueryExecutionResult(false, e.getMessage());
        }
    }

    private void applyAuth(Integration integration, RestClient.RequestBodySpec requestSpec) {
        if (!integration.hasCredentials()) {
            return;
        }
        String credentials = cryptoService.decrypt(integration.getCredentialsEncrypted());
        switch (integration.getAuthType()) {
            case BEARER ->
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials);
            case BASIC ->
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Basic "
                        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            case HEADER_CUSTOM ->
                applyCustomHeader(requestSpec, credentials);
            case NONE -> {
                /* sem auth */ }
        }
    }

    private void applyCustomHeader(RestClient.RequestBodySpec requestSpec, String credentialsJson) {
        try {
            JsonNode node = objectMapper.readTree(credentialsJson);
            requestSpec.header(node.get("header").asText(), node.get("value").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Credencial HEADER_CUSTOM inválida.", e);
        }
    }

    /**
     * Placeholders: {{today}}, {{now}} e {{question}}.
     */
    private String renderTemplate(String template, String question) {
        var now = Instant.now();
        return template
                .replace("{{today}}", now.toString().substring(0, 10))
                .replace("{{now}}", now.toString())
                .replace("{{question}}", question == null ? "" : question);
    }

    private String formatResult(Integration integration, String body) {
        String content = body == null ? "" : body;
        if (content.length() > RESPONSE_MAX_LENGTH) {
            content = content.substring(0, RESPONSE_MAX_LENGTH) + "…";
        }
        return "Resultado da integração '" + integration.getName() + "':\n" + content;
    }
}
