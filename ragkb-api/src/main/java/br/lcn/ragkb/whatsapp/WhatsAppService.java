package br.lcn.ragkb.whatsapp;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.lcn.ragkb.config.WAHAProperties;

/**
 * Camada de abstração sobre a API HTTP do WAHA (WhatsApp HTTP API).
 *
 * Contrato da WAHA usado nesta frente:
 *   GET  /api/sessions/{name}  -> status da sessão (404 se não existir)
 *   POST /api/sessions/start   -> inicia/emparelha a sessão
 *   POST /api/sendText         -> envia mensagem de texto (chatId: numero@c.us)
 *
 * O WAHA é um gateway NÃO OFICIAL do WhatsApp: o envio pode falhar por
 * banimento do número dedicado ou por sessão expirada — sempre trate como
 * estado degradado, nunca como exceção fatal para o fluxo principal da API.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String CHAT_ID_SUFFIX = "@c.us";

    private final WAHAProperties properties;
    private final RestClient restClient;

    public WhatsAppService(WAHAProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    var apiKey = properties.apiKey();
                    if (apiKey != null && !apiKey.isBlank()) {
                        request.getHeaders().set("X-Api-Key", apiKey);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    /** Situação da sessão: WORKING = conectada e apta a enviar. */
    public boolean isWorking() {
        try {
            var status = sessionStatus();
            log.debug("Status da sessão WAHA '{}': {}", properties.session(), status);
            return "WORKING".equalsIgnoreCase(status);
        } catch (WhatsAppNotConnectedException e) {
            return false;
        } catch (RestClientException e) {
            log.warn("WAHA indisponível em {}: {}", properties.baseUrl(), e.getMessage());
            return false;
        }
    }

    /**
     * Status bruto da sessão (WORKING, SCAN_QR_CODE, FAILED, ...).
     * Sessão inexistente (404 na WAHA) é estado esperado — converte para
     * WhatsAppNotConnectedException em vez de vazar HttpClientErrorException.
     */
    public String sessionStatus() {
        try {
            var response = restClient.get()
                    .uri("/api/sessions/{name}", properties.session())
                    .retrieve()
                    .body(SessionResponse.class);
            if (response == null || response.status() == null) {
                throw new WhatsAppNotConnectedException(properties.session());
            }
            return response.status();
        } catch (HttpClientErrorException.NotFound e) {
            throw new WhatsAppNotConnectedException(properties.session());
        }
    }

    /** Inicia (e sinaliza necessidade de emparelhamento via QR) a sessão. */
    public void startSession() {
        restClient.post()
                .uri("/api/sessions/start")
                .body(Map.of("name", properties.session()))
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
        if (!isWorking()) {
            throw new WhatsAppNotConnectedException(properties.session());
        }
        try {
            restClient.post()
                    .uri("/api/sendText")
                    .body(Map.of("chatId", chatId, "session", properties.session(), "text", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Mensagem WhatsApp enviada para {}", chatId);
        } catch (HttpClientErrorException.NotFound e) {
            // Sessão deletada entre o check e o envio, ou chatId inexistente na WAHA
            throw new WhatsAppNotConnectedException(properties.session());
        } catch (RestClientException e) {
            throw new WhatsAppSendException("Falha no envio via WAHA: " + e.getMessage(), e);
        }
    }

    private record SessionResponse(String name, String status) {}

    public record SendTextRequest(String chatId, String text) {}
}