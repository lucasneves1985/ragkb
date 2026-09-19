package br.lcn.ragkb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.waha")
public record WAHAProperties(
        String baseUrl,
        String apiKey,
        String session
) {
    public WAHAProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.waha.base-url é obrigatório");
        }
        if (session == null || session.isBlank()) {
            throw new IllegalStateException("app.waha.session é obrigatório");
        }
    }
}