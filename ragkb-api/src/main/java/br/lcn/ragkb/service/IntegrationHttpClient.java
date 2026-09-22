package br.lcn.ragkb.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.entity.Integration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP único para TODAS as integrações (agendadas e QUERY). Consolidado
 * na fase 2 da frente 5: antes, applyAuth/renderTemplate estavam duplicados
 * entre IntegrationExecutor e IntegrationQueryExecutor.
 *
 * Lição do IntegrationExecutor preservada: o RestClient é construído POR
 * CHAMADA — defaultHeader no builder injetado acumularia headers entre
 * execuções (o builder é instância única).
 *
 * Segurança de template: valores de {{question}} e {{param}} vêm do usuário.
 * São escapados para JSON antes da substituição — aspas, barra invertida e
 * quebras de linha não podem quebrar/inyetar no body.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationHttpClient {

    private final IntegrationCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    /**
     * Executa a chamada HTTP da integração.
     *
     * @param params valores coletados para placeholders {{param}} (podem ser
     * vazios)
     * @param question pergunta original do usuário (placeholder {{question}})
     */
    public ResponseEntity<String> call(Integration integration, String question,
            Map<String, String> params) {
        // Por chamada — ver javadoc sobre acumulação de headers
        var requestSpec = restClientBuilder.build().post().uri(integration.getUrl());
        applyAuth(integration, requestSpec);

        String body = integration.getRequestTemplate();
        if (body != null && !body.isBlank()) {
            return requestSpec.body(renderTemplate(body, question, params))
                    .retrieve()
                    .toEntity(String.class);
        }
        return requestSpec.retrieve().toEntity(String.class);
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
     * Substitui placeholders: {{today}}, {{now}}, {{question}} (escapado) e
     * {{nomeDoParam}} para cada entrada de params (escapado).
     */
    private String renderTemplate(String template, String question, Map<String, String> params) {
        var now = Instant.now();
        String rendered = template
                .replace("{{today}}", now.toString().substring(0, 10))
                .replace("{{now}}", now.toString());

        if (question != null) {
            rendered = rendered.replace("{{question}}", escapeJson(question));
        }
        if (params != null) {
            for (var entry : params.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}",
                        escapeJson(entry.getValue()));
            }
        }
        return rendered;
    }

    /**
     * Escapa o valor para uso seguro dentro de um body JSON.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' ->
                    sb.append("\\\"");
                case '\\' ->
                    sb.append("\\\\");
                case '\n' ->
                    sb.append("\\n");
                case '\r' ->
                    sb.append("\\r");
                case '\t' ->
                    sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
