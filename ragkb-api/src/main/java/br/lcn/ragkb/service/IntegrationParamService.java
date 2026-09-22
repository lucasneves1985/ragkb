package br.lcn.ragkb.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Deviação deliberada do plano original ("regex primeiro, LLM fallback"):
 * extração genérica sem LLM só cobre datas/números óbvios; para campos
 * arbitrários (região, setor, protocolo) não há como extrair sem modelo. Manter
 * as duas vias dobraria o código para 30% dos casos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationParamService {

    public record ParamField(String name, String type, String description, boolean required) {

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
            3. Devolva os valores exatamente como o usuário escreveu, sem formatação adicional.
            """;

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
     * Extrai valores da mensagem via LLM estruturado. Falha → Map.of()
     * (fail-safe: leva à pergunta ao usuário, nunca à execução com valor
     * inventado).
     */
    public Map<String, String> extract(Integration integration, String message) {
        List<ParamField> required = parseDefinition(integration.getParamsDefinition()).stream()
                .filter(ParamField::required)
                .toList();
        if (required.isEmpty() || message == null || message.isBlank()) {
            return Map.of();
        }
        String fieldsBlock = required.stream()
                .map(f -> "- %s (%s): %s".formatted(f.name(), f.type(), f.description()))
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
            // Descarta chaves que não são required e valores vazios
            Map<String, String> clean = new LinkedHashMap<>();
            for (ParamField f : required) {
                String value = extraction.valores().get(f.name());
                if (value != null && !value.isBlank()) {
                    clean.put(f.name(), value.trim());
                }
            }
            return clean;
        } catch (Exception e) {
            log.warn("Falha na extração de parâmetros da integração '{}' — tratando como ausentes: {}",
                    integration.getName(), e.getMessage());
            return Map.of();
        }
    }

    /**
     * Mensagem de solicitação DETERMINÍSTICA, montada das description dos
     * campos.
     */
    public String buildAskMessage(Integration integration, List<ParamField> missing) {
        String items = missing.stream()
                .map(f -> "- **%s**: %s".formatted(f.name(), f.description()))
                .collect(Collectors.joining("\n"));
        return "Para consultar '%s' preciso das seguintes informações:\n%s"
                .formatted(integration.getName(), items);
    }
}
