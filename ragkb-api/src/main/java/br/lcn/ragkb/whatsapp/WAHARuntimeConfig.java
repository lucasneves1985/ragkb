package br.lcn.ragkb.whatsapp;

/**
 * Configuração efetiva do WAHA em runtime: valor da tabela app_configuration
 * quando presente, com fallback para as properties (app.waha.*).
 */
public record WAHARuntimeConfig(String baseUrl, String apiKey, String session) {

    public String normalizedBaseUrl() {
        var url = baseUrl == null ? "" : baseUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}