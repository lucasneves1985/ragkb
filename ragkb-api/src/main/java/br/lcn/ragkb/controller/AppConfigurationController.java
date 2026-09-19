package br.lcn.ragkb.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.AppConfigurationValueRequest;
import br.lcn.ragkb.service.AppConfigurationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/app-configurations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AppConfigurationController {

    private final AppConfigurationService service;

    @GetMapping
    public Map<String, String> list() {
        return service.getAll();
    }

    @PutMapping("/{key}")
    public Map<String, String> update(@PathVariable String key,
                                      @RequestBody AppConfigurationValueRequest request,
                                      Authentication auth) {
        service.set(key, request.value(), auth.getName());
        return Map.of("message", "Configuração '%s' atualizada.".formatted(key));
    }
}