package br.lcn.ragkb.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.metrics.RoutingMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP único para TODAS as integrações (agendadas e QUERY). Fase 2:
 * consolidação de auth/template; fase 2b: renderização da mensagem de resposta
 * (action_template); V11: método GET/POST e placeholders na URL (path/query)
 * com URL-encoding; V11.2: renderização de LISTAS; V11.3: arrays aninhados um
 * nível ({{response.items.*.campo}}); V11.4: parsing LENIENTE como fallback —
 * APIs internas frequentemente emitem JSON com vírgula final/aspas simples, que
 * o Jackson estrito rejeita.
 *
 * Segurança de template: valores de {{question}} e {{param}} vêm do usuário. No
 * body são escapados para JSON; na URL, URL-encodados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationHttpClient {

    private static final int RESPONSE_MAX_LENGTH = 4000;
    private static final int MAX_LIST_ITEMS = 20;
    private static final int PARSE_EXCERPT_LENGTH = 300;

    private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");
    /**
     * Token de item de lista, com caminho OPCIONAL até o array antes do '*':
     * {{response.*}} -> array na raiz, item inteiro {{response.*.campo}} ->
     * array na raiz, campo do item {{response.items.*}} -> array em
     * response.items, item inteiro {{response.items.*.campo}} -> array em
     * response.items, campo do item Grupo 1: caminho até o array ("" = raiz).
     * Grupo 2: campo no item (null = item inteiro).
     */
    private static final Pattern ITEM_TOKEN
            = Pattern.compile("\\{\\{\\s*response\\.([^*}]*?)\\*(?:\\.(.+?))?\\s*}}");
    private static final Pattern ARRAY_INDEX = Pattern.compile("(.+?)\\[(\\d+)]");
    private static final String RESPONSE_ROOT = "response";

    /**
     * Parser leniente (fallback): vírgulas finais e aspas simples aceitos.
     * Trade-off consciente: tolerância a JSON malformado de APIs internas
     * (comum em montagem manual de string); NÃO é convite a relaxar o contrato
     * — o output_schema continua sendo a referência semântica.
     */
    private static final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final IntegrationCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final RoutingMetrics metrics;

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
        // a conferir em qualquer 404/400 (auth vai em header, nunca na URL).
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
            // Content-Type explícito: sem isso, o RestClient envia String body
            // como text/plain — divergência herdada do executor antigo, que
            // garantia application/json via defaultHeader (P4).
            return requestSpec.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(renderBody(body, question, params))
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
     * Renderiza a mensagem de resposta. Regras: - sem action_template: body cru
     * truncado; - template com tokens de item ({{response[.caminho].*}}): o
     * array é localizado no caminho declarado (raiz ou aninhado um nível) e o
     * bloco de itens repete por elemento, com teto e aviso explícito; - lista
     * vazia: mensagem honesta, nunca resposta vazia; - demais casos:
     * substituição única de tokens (comportamento original).
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
        JsonNode root = parseOrNull(integration.getName(), response);

        // Lista: template com tokens de item E array localizável no caminho declarado
        Matcher itemMatcher = ITEM_TOKEN.matcher(template);
        if (root != null && itemMatcher.find()) {
            String arrayPath = itemMatcher.group(1).isEmpty() ? null : itemMatcher.group(1);
            JsonNode arrayRoot = arrayPath == null ? root : navigate(root, arrayPath);
            if (arrayRoot != null && arrayRoot.isArray()) {
                return renderListTemplate(integration, template, arrayRoot);
            }
            log.warn("Caminho de lista '{}' da integração '{}' não resolve para um array — "
                    + "tokens de item ficarão vazios.",
                    arrayPath == null ? "(raiz)" : arrayPath, integration.getName());
        }

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
     * V11.2/V11.3 — renderização de lista: bloco de itens repetido por elemento
     * do array, um item por linha, com teto e aviso explícito.
     * {{response.items.length}} (ou {{response.length}} na raiz) resolve o
     * total.
     */
    private String renderListTemplate(Integration integration, String template, JsonNode arrayRoot) {
        int total = arrayRoot.size();
        if (total == 0) {
            return "A integração '" + integration.getName() + "' não retornou itens.";
        }

        String[] lines = template.split("\n", -1);
        List<Integer> itemLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (ITEM_TOKEN.matcher(lines[i]).find()) {
                itemLines.add(i);
            }
        }
        int first = itemLines.get(0);
        int last = itemLines.get(itemLines.size() - 1);
        String prefix = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, first)).stripTrailing();
        String suffix = String.join("\n", java.util.Arrays.copyOfRange(lines, last + 1, lines.length)).strip();

        int renderedCount = Math.min(total, MAX_LIST_ITEMS);
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < renderedCount; i++) {
            if (items.length() > 0) {
                items.append('\n');
            }
            items.append(renderItemBlock(lines, first, last, arrayRoot.get(i)));
        }

        StringBuilder out = new StringBuilder();
        if (!prefix.isBlank()) {
            out.append(prefix).append('\n');
        }
        out.append(items);
        if (total > renderedCount) {
            out.append(String.format("\n… e mais %d itens (mostrando %d de %d).",
                    total - renderedCount, renderedCount, total));
        }
        if (!suffix.isBlank()) {
            out.append('\n').append(suffix);
        }
        return truncate(out.toString());
    }

    /**
     * Renderiza o bloco de itens (linhas first..last) contra UM item do array.
     */
    private String renderItemBlock(String[] lines, int first, int last, JsonNode item) {
        StringBuilder rendered = new StringBuilder();
        for (int i = first; i <= last; i++) {
            if (rendered.length() > 0) {
                rendered.append('\n');
            }
            Matcher matcher = ITEM_TOKEN.matcher(lines[i]);
            StringBuffer lineOut = new StringBuffer();
            while (matcher.find()) {
                String fieldPath = matcher.group(2);
                String value = resolveItemToken(item, fieldPath);
                matcher.appendReplacement(lineOut, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(lineOut);
            rendered.append(lineOut);
        }
        return rendered.toString();
    }

    private String resolveItemToken(JsonNode item, String fieldPath) {
        if (fieldPath == null || fieldPath.isBlank()) {
            // {{response[.caminho].*}} — o item inteiro
            return item.isValueNode() ? item.asText() : item.toString();
        }
        JsonNode node = navigate(item, fieldPath);
        if (node == null || node.isMissingNode() || node.isNull()) {
            log.warn("Token de item '{{response.*.{}' não encontrado no item — substituído por vazio.",
                    fieldPath);
            return "";
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

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

    /**
     * Parse do body da resposta, em duas tentativas: estrito → leniente. P3:
     * cada desfecho vira counter — parse(outcome=lenient) alto é o medidor
     * objetivo de API desviando do contrato JSON.
     */
    private JsonNode parseOrNull(String integrationName, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            metrics.parseOutcome(integrationName, "empty");
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            metrics.parseOutcome(integrationName, "strict");
            return node;
        } catch (JsonProcessingException strict) {
            try {
                JsonNode lenient = LENIENT_MAPPER.readTree(responseBody);
                metrics.parseOutcome(integrationName, "lenient");
                log.warn("Resposta da integração parseada em modo LENIENTE (JSON com desvios, "
                        + "ex.: vírgula final) — avalie corrigir a origem.");
                return lenient;
            } catch (JsonProcessingException lenientEx) {
                metrics.parseOutcome(integrationName, "invalid");
                log.warn("Resposta da integração NÃO é JSON (nem leniente) — tokens ficarão vazios. "
                        + "Trecho inicial do body: [{}]",
                        excerpt(responseBody));
                return null;
            }
        }
    }

    private String excerpt(String body) {
        String flat = body.replace("\n", "\\n").replace("\r", "").replace("\t", " ");
        return flat.length() <= PARSE_EXCERPT_LENGTH ? flat : flat.substring(0, PARSE_EXCERPT_LENGTH) + "…";
    }

    private String resolveToken(String token, JsonNode root, String rawResponse) {
        if (RESPONSE_ROOT.equals(token)) {
            return rawResponse;
        }
        // {{caminho.length}} — total de itens quando o caminho resolve um array
        if (token.endsWith(".length") && root != null) {
            String path = token.substring(RESPONSE_ROOT.length() + 1,
                    token.length() - ".length".length());
            JsonNode node = path.isBlank() ? root : navigate(root, path);
            if (node != null && node.isArray()) {
                return String.valueOf(node.size());
            }
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
