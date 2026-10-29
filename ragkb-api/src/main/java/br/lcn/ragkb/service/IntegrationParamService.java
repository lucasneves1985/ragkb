package br.lcn.ragkb.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.entity.Integration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fase 2 da frente 5: coleta de parâmetros baseada em params_definition (JSON
 * Schema). Extração dos valores via LLM estruturado (uma chamada); a mensagem
 * de solicitação ao usuário é DETERMINÍSTICA (montada das description dos
 * campos — zero LLM).
 *
 * V11.1 — normalização de datas: APIs externas esperam formatos canônicos
 * (normalmente AAAA-MM-DD) e o usuário brasileiro escreve DD/MM/AAAA. Duas
 * camadas: (1) o prompt instrui o LLM a converter quando o campo declara
 * "format": "date"; (2) normalização determinística por parsing após a extração
 * — se o LLM escorregar, o código conserta. Datas RELATIVAS ("hoje", "ontem")
 * NÃO são resolvidas — limitação consciente documentada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationParamService {

    public record ParamField(String name, String type, String description,
            String format, boolean required) {

    }

    /**
     * Retorno estruturado da extração LLM.
     */
    public record ParamExtraction(Map<String, String> valores) {

    }

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            Você extrai valores de parâmetros de uma mensagem de usuário.
            REGRAS:
            1. Para cada parâmetro listado, extraia o valor da mensagem se ele estiver presente.
            2. Se o valor NÃO estiver na mensagem, OMITA a chave — NUNCA invente valor.
            3. Para parâmetros de DATA (format=date), converta o valor para o formato
               AAAA-MM-DD: "21/09/2026" vira "2026-09-21"; "21-09-2026" vira "2026-09-21".
            4. Para os demais parâmetros, devolva o valor exatamente como o usuário escreveu.
            """;

    /**
     * Formatos de entrada aceitos na normalização determinística (mais comuns
     * BR).
     */
    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.of("pt", "BR")),
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.of("pt", "BR")),
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.of("pt", "BR")),
            DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.of("pt", "BR")),
            DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.of("pt", "BR")));
    private static final DateTimeFormatter ISO_OUTPUT
            = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.of("pt", "BR"));

    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public List<ParamField> parseDefinition(String paramsDefinition) {
        if (paramsDefinition == null || paramsDefinition.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(paramsDefinition);
            JsonNode properties = root.get("properties");
            if (properties == null || !properties.isObject()) {
                return List.of();
            }
            List<String> required = new ArrayList<>();
            JsonNode requiredNode = root.get("required");
            if (requiredNode != null && requiredNode.isArray()) {
                requiredNode.forEach(n -> required.add(n.asText()));
            }
            List<ParamField> fields = new ArrayList<>();
            properties.fieldNames().forEachRemaining(name -> {
                JsonNode prop = properties.get(name);
                fields.add(new ParamField(
                        name,
                        prop.has("type") ? prop.get("type").asText() : "string",
                        prop.has("description") ? prop.get("description").asText() : name,
                        prop.has("format") ? prop.get("format").asText() : null,
                        required.contains(name)));
            });
            return fields;
        } catch (Exception e) {
            log.warn("params_definition inválido — tratado como sem parâmetros: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean hasRequiredParams(Integration integration) {
        return parseDefinition(integration.getParamsDefinition()).stream()
                .anyMatch(ParamField::required);
    }

    public List<ParamField> findMissingRequired(Integration integration,
            Map<String, String> provided) {
        return parseDefinition(integration.getParamsDefinition()).stream()
                .filter(ParamField::required)
                .filter(f -> provided.get(f.name()) == null || provided.get(f.name()).isBlank())
                .toList();
    }

    /**
     * Extrai valores da mensagem via LLM estruturado + normalização por campo.
     * Falha → Map.of() (fail-safe: leva à pergunta ao usuário, nunca à execução
     * com valor inventado).
     */
    public Map<String, String> extract(Integration integration, String message) {
        List<ParamField> required = parseDefinition(integration.getParamsDefinition()).stream()
                .filter(ParamField::required)
                .toList();
        if (required.isEmpty() || message == null || message.isBlank()) {
            return Map.of();
        }
        String fieldsBlock = required.stream()
                .map(f -> "- %s (%s%s): %s".formatted(
                f.name(), f.type(),
                "date".equals(f.format()) ? ", format=date" : "",
                f.description()))
                .collect(Collectors.joining("\n"));
        try {
            ParamExtraction extraction = chatClient.prompt()
                    .system(EXTRACTION_SYSTEM_PROMPT)
                    .user("Parâmetros:\n" + fieldsBlock + "\n\nMensagem do usuário: " + message)
                    .call()
                    .entity(ParamExtraction.class);
            if (extraction == null || extraction.valores() == null) {
                return Map.of();
            }
            // Descarta chaves que não são required, valores vazios e normaliza datas
            Map<String, String> clean = new LinkedHashMap<>();
            for (ParamField f : required) {
                String value = extraction.valores().get(f.name());
                if (value == null || value.isBlank()) {
                    continue;
                }
                String normalized = "date".equals(f.format()) ? normalizeDate(value.trim()) : value.trim();
                if (normalized != null) {
                    clean.put(f.name(), normalized);
                }
                // normalizeDate null → valor inválido tratado como ausente:
                // cai na pergunta ao usuário em vez de executar com lixo
            }
            return clean;
        } catch (Exception e) {
            log.warn("Falha na extração de parâmetros da integração '{}' — tratando como ausentes: {}",
                    integration.getName(), e.getMessage());
            return Map.of();
        }
    }

    /**
     * Normaliza datas de entrada para ISO AAAA-MM-DD. Retorna null se o valor
     * não parsear em NENHUM formato conhecido — inclui datas relativas ("hoje",
     * "ontem", "amanhã"), que são limitação consciente desta versão
     * (resolvê-las exigiria injetar o relógio na extração; vira caso de
     * PARAM_REQUIRED com mensagem clara em vez de execução com valor errado).
     */
    String normalizeDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // Já está em ISO? (caminho mais comum quando o LLM já converteu)
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                return LocalDate.parse(value, ISO_OUTPUT).toString();
            } catch (DateTimeParseException e) {
                return null; // AAAA-MM-DD com mês/dia inválidos (ex.: 2026-13-45)
            }
        }
        for (DateTimeFormatter formatter : INPUT_FORMATS) {
            try {
                return LocalDate.parse(value, formatter).format(ISO_OUTPUT);
            } catch (DateTimeParseException e) {
                // tenta o próximo formato
            }
        }
        log.warn("Valor de data não reconhecido — tratado como ausente: '{}'", value);
        return null;
    }

    /**
     * Mensagem de solicitação DETERMINÍSTICA, montada das description dos
     * campos.
     */
    public String buildAskMessage(Integration integration, List<ParamField> missing) {
        String items = missing.stream()
                .map(this::askLine)
                .collect(Collectors.joining("\n"));
        return "Para consultar '%s' preciso das seguintes informações:\n%s"
                .formatted(integration.getName(), items);
    }

    /**
     * Campos de data pedem explicitamente o formato canônico — reduz a chance
     * de o usuário responder no formato errado já na origem.
     */
    private String askLine(ParamField f) {
        String base = "- **%s**: %s".formatted(f.name(), f.description());
        return "date".equals(f.format())
                ? base + " (informe no formato AAAA-MM-DD, ex.: 2026-09-21)"
                : base;
    }
}
