package br.lcn.ragkb.whatsapp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Camada de abstração sobre a API HTTP do WAHA (WhatsApp HTTP API).
 *
 * Contrato da WAHA usado nesta frente:
 *   GET  /api/sessions/{name}  -> status da sessão (404 se não existir)
 *   POST /api/sessions/start   -> inicia/emparelha a sessão
 *   POST /api/sendText         -> envia mensagem de texto (chatId: numero@c.us)
 *
 * Configuração dual-source: WhatsAppConfigResolver resolve base-url, api-key e
 * session do banco (app_configuration) com fallback para properties. O RestClient
 * é construído por chamada com URL absoluta — trocar a config no banco vale na
 * hora, sem restart.
 *
 * O WAHA é um gateway NÃO OFICIAL do WhatsApp: o envio pode falhar por banimento
 * do número dedicado ou por sessão expirada — sempre trate como estado degradado,
 * nunca como exceção fatal para o fluxo principal da API.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private static final String CHAT_ID_SUFFIX = "@c.us";

    private final WhatsAppConfigResolver configResolver;
    private final RestClient.Builder restClientBuilder;

    public WhatsAppService(WhatsAppConfigResolver configResolver, RestClient.Builder restClientBuilder) {
        this.configResolver = configResolver;
        this.restClientBuilder = restClientBuilder;
    }

    private RestClient client(WAHARuntimeConfig config) {
        return restClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    if (config.apiKey() != null && !config.apiKey().isBlank()) {
                        request.getHeaders().set("X-Api-Key", config.apiKey());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /** Situação da sessão: WORKING = conectada e apta a enviar. */
    public boolean isWorking() {
        try {
            var status = sessionStatus();
            log.debug("Status da sessão WAHA: {}", status);
            return "WORKING".equalsIgnoreCase(status);
        } catch (WhatsAppNotConnectedException e) {
            return false;
        } catch (RestClientException e) {
            log.warn("WAHA indisponível: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Status bruto da sessão (WORKING, SCAN_QR_CODE, FAILED, ...).
     * Sessão inexistente (404 na WAHA) é estado esperado — converte para
     * WhatsAppNotConnectedException em vez de vazar HttpClientErrorException.
     */
    public String sessionStatus() {
        var config = configResolver.resolve();
        try {
            var response = client(config).get()
                    .uri(config.normalizedBaseUrl() + "/api/sessions/{name}", config.session())
                    .retrieve()
                    .body(SessionResponse.class);
            if (response == null || response.status() == null) {
                throw new WhatsAppNotConnectedException(config.session());
            }
            return response.status();
        } catch (HttpClientErrorException.NotFound e) {
            throw new WhatsAppNotConnectedException(config.session());
        }
    }

    /** Inicia (e sinaliza necessidade de emparelhamento via QR) a sessão. */
    public void startSession() {
        var config = configResolver.resolve();
        client(config).post()
                .uri(config.normalizedBaseUrl() + "/api/sessions/start")
                .body(Map.of("name", config.session()))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Envia mensagem de texto. chatId deve estar no formato
     * "&lt;numero-internacional-somente-digitos&gt;@c.us".
     *
     * @throws WhatsAppNotConnectedException se a sessão não estiver WORKING
     * @throws WhatsAppSendException         se a WAHA rejeitar o envio
     */
    public void sendText(String chatId, String message) {
        if (chatId == null || !chatId.endsWith(CHAT_ID_SUFFIX)) {
            throw new WhatsAppSendException(
                    "chatId inválido: esperado formato '5554999999999@c.us', recebido: " + chatId);
        }
        var config = configResolver.resolve();
        if (!isWorking()) {
            throw new WhatsAppNotConnectedException(config.session());
        }
        try {
            client(config).post()
                    .uri(config.normalizedBaseUrl() + "/api/sendText")
                    .body(Map.of("chatId", chatId, "session", config.session(), "text", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mensagem WhatsApp enviada para {}", chatId);
        } catch (HttpClientErrorException.NotFound e) {
            // Sessão deletada entre o check e o envio, ou chatId inexistente na WAHA
            throw new WhatsAppNotConnectedException(config.session());
        } catch (RestClientException e) {
            throw new WhatsAppSendException("Falha no envio via WAHA: " + e.getMessage(), e);
        }
    }

    private record SessionResponse(String name, String status) {}
}