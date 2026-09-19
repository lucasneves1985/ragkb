package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.CreateIntegrationRequest;
import br.lcn.ragkb.dto.IntegrationDto;
import br.lcn.ragkb.dto.IntegrationExecutionDto;
import br.lcn.ragkb.dto.UpdateIntegrationRequest;
import br.lcn.ragkb.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CRUD de integrações externas. ADMIN escreve; EDITOR pode ler (precisa ver
 * integrações QUERY para testes na frente 5).
 */
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationDto create(@Valid @RequestBody CreateIntegrationRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<IntegrationDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public IntegrationDto get(@PathVariable String id) {
        return service.get(id);
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<IntegrationExecutionDto> listExecutions(@PathVariable String id) {
        return service.listExecutions(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationDto update(@PathVariable String id,
            @Valid @RequestBody UpdateIntegrationRequest request,
            Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
