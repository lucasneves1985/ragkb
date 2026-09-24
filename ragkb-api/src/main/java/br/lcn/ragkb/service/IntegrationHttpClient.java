package br.lcn.ragkb.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * na fase 2 da frente 5; fase 2b adicionou a renderização da mensagem de
 * resposta (action_template); V11 adicionou método GET/POST e placeholders na
 * URL (path/query) com URL-encoding.
 *
 * Segurança de template: valores de {{question}} e {{param}} vêm do usuário. No
 * body são escapados para JSON; na URL, URL-encodados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationHttpClient {

    private static final int RESPONSE_MAX_LENGTH = 4000;

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    private static final Pattern ARRAY_INDEX = Pattern.compile("(.+?)\\[(\\d+)]");
    private static final String RESPONSE_ROOT = "response";

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
        // Por chamada — defaultHeader no builder injetado acumularia headers
        RestClient client = restClientBuilder.build();
        String url = renderUrl(integration.getUrl(), question, params);

        // Telemetria de diagnóstico: a URL FINAL renderizada é a primeira coisa
        // a conferir em qualquer 404/400 da integração (não loga credenciais —
        // auth vai em header, nunca na URL).
        log.info(String.format("[integration] %s %s", integration.getHttpMethod(), url));

        if (integration.getHttpMethod() == br.lcn.ragkb.entity.IntegrationHttpMethod.GET) {
            if (integration.getRequestTemplate() != null && !integration.getRequestTemplate().isBlank()) {
                log.warn("Integração '{}' é GET e tem request_template — o body será IGNORADO "
                        + "(parâmetros devem ir na URL: path ou query).", integration.getName());
            }
            var spec = client.get().uri(url);
            applyAuth(integration, spec);
            return spec.retrieve().toEntity(String.class);
        }

        var requestSpec = client.post().uri(url);
        applyAuth(integration, requestSpec);

        String body = integration.getRequestTemplate();
        if (body != null && !body.isBlank()) {
            return requestSpec.body(renderBody(body, question, params))
                    .retrieve()
                    .toEntity(String.class);
        }
        return requestSpec.retrieve().toEntity(String.class);
    }

    /**
     * Renderiza a URL: builtins ({{today}}/{{now}}) e placeholders de
     * parâmetro/{{question}} com URL-encoding (cobre path e query).
     */
    private String renderUrl(String urlTemplate, String question, Map<String, String> params) {
        var now = Instant.now();
        String url = urlTemplate
                .replace("{{today}}", now.toString().substring(0, 10))
                .replace("{{now}}", now.toString());

        Matcher matcher = TEMPLATE_TOKEN.matcher(url);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            String raw = resolveUserValue(token, question, params);
            String encoded = urlEncode(raw);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(encoded));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String resolveUserValue(String token, String question, Map<String, String> params) {
        if ("question".equalsIgnoreCase(token)) {
            return question == null ? "" : question;
        }
        if (params != null && params.containsKey(token)) {
            String value = params.get(token);
            return value == null ? "" : value;
        }
        log.warn("Placeholder '{{{{{}}}}}' na URL sem valor fornecido — substituído por vazio.", token);
        return "";
    }

    /**
     * URLEncoder é form-encoding ('+' para espaço) — corrige para %20
     * (path/query).
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /**
     * Renderiza a mensagem de resposta da integração: com action_template,
     * extrai os campos do JSON da resposta; sem template, body cru truncado.
     */
    public String renderResponseMessage(Integration integration, String responseBody) {
        String response = responseBody == null ? "" : responseBody;

        if (integration.getActionTemplate() == null || integration.getActionTemplate().isBlank()) {
            return truncate(response);
        }

        var now = Instant.now();
        String template = integration.getActionTemplate()
                .replace("{{today}}", now.toString().substring(0, 10))
                .replace("{{now}}", now.toString());
        JsonNode root = parseOrNull(response);
        Matcher matcher = TEMPLATE_TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            String value = resolveToken(token, root, response);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return truncate(rendered.toString());
    }

    /**
     * applyAuth aceita RequestHeadersSpec: interface comum de RestClient.get()
     * (RequestHeadersSpec) e post() (RequestBodySpec).
     */
    private void applyAuth(Integration integration, RestClient.RequestHeadersSpec<?> spec) {
        if (!integration.hasCredentials()) {
            return;
        }
        String credentials = cryptoService.decrypt(integration.getCredentialsEncrypted());
        switch (integration.getAuthType()) {
            case BEARER ->
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + credentials);
            case BASIC ->
                spec.header(HttpHeaders.AUTHORIZATION, "Basic "
                        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            case HEADER_CUSTOM ->
                applyCustomHeader(spec, credentials);
            case NONE -> {
                /* sem auth */ }
        }
    }

    private void applyCustomHeader(RestClient.RequestHeadersSpec<?> spec, String credentialsJson) {
        try {
            JsonNode node = objectMapper.readTree(credentialsJson);
            spec.header(node.get("header").asText(), node.get("value").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Credencial HEADER_CUSTOM inválida.", e);
        }
    }

    /**
     * Substitui placeholders do request_template (POST): {{today}}, {{now}},
     * {{question}} (escapado) e {{nomeDoParam}} para cada entrada de params
     * (escapado).
     */
    private String renderBody(String template, String question, Map<String, String> params) {
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

    private JsonNode parseOrNull(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            log.warn("Resposta da integração não é JSON válido; tokens {{response.*}} ficarão vazios.");
            return null;
        }
    }

    private String resolveToken(String token, JsonNode root, String rawResponse) {
        if (RESPONSE_ROOT.equals(token)) {
            return rawResponse;
        }
        if (!token.startsWith(RESPONSE_ROOT + ".")) {
            log.warn("Token '{{{{{}}}}}' desconhecido no action_template — será substituído por vazio.", token);
            return "";
        }
        JsonNode node = navigate(root, token.substring(RESPONSE_ROOT.length() + 1));
        if (node == null || node.isMissingNode() || node.isNull()) {
            log.warn("Token '{{{{{}}}}}' não encontrado no JSON da resposta — será substituído por vazio.", token);
            return "";
        }
        // Textual via asText() (sem aspas); objetos/arrays via toString()
        return node.isValueNode() ? node.asText() : node.toString();
    }

    /**
     * Navega por caminho pontilhado, com suporte a índice de array (items[0] ou
     * items.0).
     */
    private JsonNode navigate(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode node = root;
        for (String part : path.split("\\.")) {
            if (node == null) {
                return null;
            }
            Matcher index = ARRAY_INDEX.matcher(part);
            if (index.matches()) {
                node = node.get(index.group(1));
                if (node != null && node.isArray()) {
                    node = node.get(Integer.parseInt(index.group(2)));
                }
            } else if (node.isArray() && part.matches("\\d+")) {
                node = node.get(Integer.parseInt(part));
            } else {
                node = node.get(part);
            }
        }
        return node;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= RESPONSE_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, RESPONSE_MAX_LENGTH) + "…";
    }
}
