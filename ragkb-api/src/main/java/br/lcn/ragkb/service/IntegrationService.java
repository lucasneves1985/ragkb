package br.lcn.ragkb.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.dto.CreateIntegrationRequest;
import br.lcn.ragkb.dto.IntegrationDto;
import br.lcn.ragkb.dto.UpdateIntegrationRequest;
import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationType;
import br.lcn.ragkb.exception.DuplicateIntegrationNameException;
import br.lcn.ragkb.exception.IntegrationNotFoundException;
import br.lcn.ragkb.exception.InvalidIntegrationException;
import br.lcn.ragkb.repository.IntegrationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationRepository repository;
    private final IntegrationCryptoService cryptoService;
    private final ObjectMapper objectMapper;

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
                request.outputSchema(), request.paramsDefinition());

        Integration integration = new Integration(
                request.name().trim(), request.description(), request.url().trim(),
                request.authType(), request.integrationType(),
                request.scheduleCron(), request.scheduleTimezone(), request.scheduleIntervalSeconds(),
                request.contextDescription(), request.requestTemplate(),
                request.outputSchema(), request.paramsDefinition(),
                request.active(), username);

        if (request.credentials() != null && !request.credentials().isBlank()) {
            integration.assignCredentials(cryptoService.encrypt(request.credentials()));
        }
        return IntegrationDto.from(repository.save(integration));
    }

    @Transactional
    public IntegrationDto update(String id, UpdateIntegrationRequest request, String username) {
        Integration integration = find(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateIntegrationNameException(request.name());
        }
        validate(request.url(), request.integrationType(), request.scheduleCron(),
                request.scheduleIntervalSeconds(), request.contextDescription(),
                request.outputSchema(), request.paramsDefinition());

        integration.updateCore(
                request.name().trim(), request.description(), request.url().trim(),
                request.authType(), request.integrationType(),
                request.scheduleCron(), request.scheduleTimezone(), request.scheduleIntervalSeconds(),
                request.contextDescription(), request.requestTemplate(),
                request.outputSchema(), request.paramsDefinition(), request.active());

        if (request.credentials() != null && !request.credentials().isBlank()) {
            integration.assignCredentials(cryptoService.encrypt(request.credentials()));
        }
        return IntegrationDto.from(repository.save(integration));
    }

    @Transactional
    public void delete(String id) {
        Integration integration = find(id);
        repository.delete(integration);
    }

    private Integration find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IntegrationNotFoundException(id));
    }

    private void validate(String url, IntegrationType type, String cron, Long intervalSeconds,
                          String contextDescription, String outputSchema, String paramsDefinition) {
        assertAllowedUrl(url);
        if (type == IntegrationType.SCHEDULED
                && (cron == null || cron.isBlank()) && intervalSeconds == null) {
            throw new InvalidIntegrationException(
                    "Integrações SCHEDULED exigem scheduleCron ou scheduleIntervalSeconds.");
        }
        if (type == IntegrationType.QUERY
                && (contextDescription == null || contextDescription.isBlank())) {
            throw new InvalidIntegrationException(
                    "Integrações QUERY exigem contextDescription (rota do LLM na frente 5).");
        }
        assertJsonObject(outputSchema, "outputSchema");
        assertJsonObject(paramsDefinition, "paramsDefinition");
    }

    /**
     * Bloqueio básico de SSRF: só http/https e sem hosts internos/privados.
     * Limitação conhecida: DNS rebinding (host que resolve para IP público na
     * validação e privado na chamada) exige resolução fixada no momento da
     * execução — fica como endurecimento futuro junto ao executor da frente 3.
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
        String h = host.toLowerCase();
        try {
            InetAddress address = InetAddress.getByName(h);
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