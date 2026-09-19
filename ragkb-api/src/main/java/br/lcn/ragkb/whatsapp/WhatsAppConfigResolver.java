package br.lcn.ragkb.whatsapp;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.config.WAHAProperties;
import br.lcn.ragkb.service.AppConfigurationService;
import lombok.RequiredArgsConstructor;

/**
 * Resolve a configuração do WAHA por chamada: banco (app_configuration)
 * sobrescreve properties. Leitura direta por chamada — operação barata
 * (PK lookup) e evita cache inválido quando o admin troca a config.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppConfigResolver {

    private final AppConfigurationService appConfigurationService;
    private final WAHAProperties properties;

    public WAHARuntimeConfig resolve() {
        return new WAHARuntimeConfig(
                firstNonBlank(appConfigurationService.get(AppConfigurationService.KEY_WAHA_BASE_URL),
                        properties.baseUrl()),
                firstNonBlank(appConfigurationService.get(AppConfigurationService.KEY_WAHA_API_KEY),
                        properties.apiKey()),
                firstNonBlank(appConfigurationService.get(AppConfigurationService.KEY_WAHA_SESSION),
                        properties.session()));
    }

    private String firstNonBlank(java.util.Optional<String> preferred, String fallback) {
        if (preferred.isPresent() && !preferred.get().isBlank()) {
            return preferred.get();
        }
        return fallback;
    }
}