package br.lcn.ragkb.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.dto.CreateIntegrationRequest;
import br.lcn.ragkb.dto.IntegrationDto;
import br.lcn.ragkb.dto.IntegrationExecutionDto;
import br.lcn.ragkb.dto.UpdateIntegrationRequest;
import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationActionType;
import br.lcn.ragkb.entity.IntegrationType;
import br.lcn.ragkb.exception.DuplicateIntegrationNameException;
import br.lcn.ragkb.exception.IntegrationNotFoundException;
import br.lcn.ragkb.exception.InvalidIntegrationException;
import br.lcn.ragkb.repository.IntegrationExecutionRepository;
import br.lcn.ragkb.repository.IntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationRepository repository;
    private final IntegrationExecutionRepository executionRepository;
    private final IntegrationCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final IntegrationEmbeddingStore embeddingStore;

    @Transactional(readOnly = true)
    public List<IntegrationDto> list() {
        return repository.findAll().stream().map(IntegrationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public IntegrationDto get(String id) {
        return IntegrationDto.from(find(id));
    }

    @Transactional
    public IntegrationDto create(CreateIntegrationRequest request, String username) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateIntegrationNameException(request.name());
        }
        validate(request.url(), request.integrationType(), request.scheduleCron(),
                request.scheduleIntervalSeconds(), request.contextDescription(),
                request.outputSchema(), request.paramsDefinition(),
                request.actionType(), request.actionTarget(), request.credentials(), request.authType());

        Integration integration = new Integration(
                request.name().trim(), request.description(), request.url().trim(),
                request.authType(), request.integrationType(),
                request.scheduleCron(), request.scheduleTimezone(), request.scheduleIntervalSeconds(),
                request.contextDescription(), request.requestTemplate(),
                request.outputSchema(), request.paramsDefinition(),
                request.actionType(), request.actionTarget(), request.actionTemplate(),
                request.active(), username);

        if (request.credentials() != null && !request.credentials().isBlank()) {
            integration.assignCredentials(cryptoService.encrypt(request.credentials()));
        }
        Integration saved = repository.save(integration);
        refreshDescriptionEmbedding(saved);
        return IntegrationDto.from(saved);
    }

    @Transactional
    public IntegrationDto update(String id, UpdateIntegrationRequest request, String username) {
        Integration integration = find(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateIntegrationNameException(request.name());
        }
        validate(request.url(), request.integrationType(), request.scheduleCron(),
                request.scheduleIntervalSeconds(), request.contextDescription(),
                request.outputSchema(), request.paramsDefinition(),
                request.actionType(), request.actionTarget(), request.credentials(), request.authType());

        integration.updateCore(
                request.name().trim(), request.description(), request.url().trim(),
                request.authType(), request.integrationType(),
                request.scheduleCron(), request.scheduleTimezone(), request.scheduleIntervalSeconds(),
                request.contextDescription(), request.requestTemplate(),
                request.outputSchema(), request.paramsDefinition(),
                request.actionType(), request.actionTarget(), request.actionTemplate(),
                request.active());

        if (request.credentials() != null && !request.credentials().isBlank()) {
            integration.assignCredentials(cryptoService.encrypt(request.credentials()));
        }
        Integration saved = repository.save(integration);
        refreshDescriptionEmbedding(saved);
        return IntegrationDto.from(saved);
    }

    @Transactional
    public void delete(String id) {
        repository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public List<IntegrationExecutionDto> listExecutions(String id) {
        find(id); // 404 se a integração não existir
        return executionRepository.findTop50ByIntegrationIdOrderByStartedAtDesc(id).stream()
                .map(IntegrationExecutionDto::from)
                .toList();
    }

    /**
     * Gera/grava o embedding da context_description (gate de roteamento). Só
     * para QUERY com descrição; caso contrário limpa. Falha de embedding NÃO
     * bloqueia o save — a integração fica sem participar do gate até a próxima
     * edição (degradado consciente).
     */
    private void refreshDescriptionEmbedding(Integration integration) {
        try {
            if (integration.getIntegrationType() == IntegrationType.QUERY
                    && integration.getContextDescription() != null
                    && !integration.getContextDescription().isBlank()) {
                float[] embedding = embeddingModel.embed(integration.getContextDescription());
                embeddingStore.updateEmbedding(integration.getId(), embedding);
            } else {
                embeddingStore.clearEmbedding(integration.getId());
            }
        } catch (Exception e) {
            log.warn("Falha ao gerar embedding da descrição da integração '{}' — "
                    + "ela não participará do roteamento até a próxima edição: {}",
                    integration.getName(), e.getMessage());
        }
    }

    private Integration find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException(id));
    }

    private void validate(String url, IntegrationType type, String cron, Long intervalSeconds,
            String contextDescription, String outputSchema, String paramsDefinition,
            IntegrationActionType actionType, String actionTarget,
            String credentials, br.lcn.ragkb.entity.IntegrationAuthType authType) {
        assertAllowedUrl(url);
        if (type == IntegrationType.SCHEDULED
                && (cron == null || cron.isBlank()) && intervalSeconds == null) {
            throw new InvalidIntegrationException(
                    "Integrações SCHEDULED exigem scheduleCron ou scheduleIntervalSeconds.");
        }
        if (type == IntegrationType.QUERY
                && (contextDescription == null || contextDescription.isBlank())) {
            throw new InvalidIntegrationException(
                    "Integrações QUERY exigem contextDescription (base do roteamento).");
        }
        assertJsonObject(outputSchema, "outputSchema");
        assertJsonObject(paramsDefinition, "paramsDefinition");
        validateAction(actionType, actionTarget);
        validateCredentials(authType, credentials);
        if (cron != null && !cron.isBlank()) {
            assertValidCron(cron);
        }
    }

    private void validateAction(IntegrationActionType actionType, String actionTarget) {
        if (actionType == null) {
            return;
        }
        if (actionType == IntegrationActionType.EMAIL) {
            if (actionTarget == null || !actionTarget.contains("@")) {
                throw new InvalidIntegrationException(
                        "Ação EMAIL exige actionTarget com e-mail de destino válido.");
            }
        }
        if (actionType == IntegrationActionType.WHATSAPP) {
            if (actionTarget == null || !actionTarget.endsWith("@c.us")) {
                throw new InvalidIntegrationException(
                        "Ação WHATSAPP exige actionTarget no formato '55DDNNNNNNNNN@c.us'.");
            }
        }
    }

    /**
     * HEADER_CUSTOM exige credenciais em JSON: {"header": "X-Api-Key", "value":
     * "..."}
     */
    private void validateCredentials(br.lcn.ragkb.entity.IntegrationAuthType authType,
            String credentials) {
        if (credentials == null || credentials.isBlank()
                || authType != br.lcn.ragkb.entity.IntegrationAuthType.HEADER_CUSTOM) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(credentials);
            if (!node.isObject() || !node.hasNonNull("header") || !node.hasNonNull("value")) {
                throw new InvalidIntegrationException(
                        "Para HEADER_CUSTOM, credentials deve ser JSON {\"header\": \"...\", \"value\": \"...\"}.");
            }
        } catch (InvalidIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidIntegrationException("credentials não é um JSON válido para HEADER_CUSTOM.");
        }
    }

    private void assertValidCron(String cron) {
        try {
            new org.springframework.scheduling.support.CronTrigger(cron);
        } catch (IllegalArgumentException e) {
            throw new InvalidIntegrationException("scheduleCron inválido: " + e.getMessage());
        }
    }

    /**
     * Bloqueio básico de SSRF: só http/https e sem hosts internos/privados.
     * Limitação conhecida: DNS rebinding exige resolução fixada no momento da
     * execução — endurecimento futuro junto ao executor.
     */
    private void assertAllowedUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidIntegrationException("URL inválida: " + url);
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new InvalidIntegrationException("URL deve usar http ou https: " + url);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidIntegrationException("URL sem host válido: " + url);
        }
        try {
            InetAddress address = InetAddress.getByName(host.toLowerCase());
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw new InvalidIntegrationException(
                        "URL aponta para endereço interno/privado (bloqueio SSRF): " + host);
            }
        } catch (UnknownHostException e) {
            throw new InvalidIntegrationException("Host não resolvido: " + host);
        }
    }

    private void assertJsonObject(String json, String field) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject()) {
                throw new InvalidIntegrationException(field + " deve ser um objeto JSON.");
            }
        } catch (InvalidIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidIntegrationException(field + " não é um JSON válido.");
        }
    }
}
