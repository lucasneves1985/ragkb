package br.lcn.ragkb.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.entity.AppConfiguration;
import br.lcn.ragkb.repository.AppConfigurationRepository;
import lombok.RequiredArgsConstructor;

/**
 * Configurações runtime da aplicação (key/value). Fonte de verdade do
 * WAHA a partir daqui; as properties (app.waha.*) viram fallback.
 */
@Service
@RequiredArgsConstructor
public class AppConfigurationService {

    public static final String KEY_WAHA_BASE_URL = "waha.base_url";
    public static final String KEY_WAHA_API_KEY = "waha.api_key";
    public static final String KEY_WAHA_SESSION = "waha.session";

    private final AppConfigurationRepository repository;

    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        return repository.findById(key).map(AppConfiguration::getValue);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(AppConfiguration::getKey, c -> c.getValue() == null ? "" : c.getValue()));
    }

    @Transactional
    public void set(String key, String value, String updatedBy) {
        var config = repository.findById(key)
                .orElseGet(() -> new AppConfiguration(key, null, updatedBy));
        config.update(value, updatedBy);
        repository.save(config);
    }
}