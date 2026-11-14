package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.CreateProjectRequest;
import br.lcn.ragkb.dto.ProjectDto;
import br.lcn.ragkb.dto.UpdateProjectRequest;
import br.lcn.ragkb.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * Escopo pessoal: sem @PreAuthorize — o SecurityConfig já exige autenticação
 * para qualquer request (anyRequest().authenticated()). As regras de
 * permissão (owner/participante/legado) são enforcement do ProjectService.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @PostMapping
    public ProjectDto create(@Valid @RequestBody CreateProjectRequest request,
            Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @GetMapping
    public List<ProjectDto> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable String id, Authentication authentication) {
        return service.get(id, authentication.getName());
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable String id, @Valid @RequestBody UpdateProjectRequest request,
            Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        service.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ── Compartilhamento (owner-only; enforcement no service) ──

    public record ShareRequest(@NotBlank String username) {
    }

    @PostMapping("/{id}/shares")
    public ProjectDto share(@PathVariable String id, @Valid @RequestBody ShareRequest request,
            Authentication authentication) {
        return service.share(id, authentication.getName(), request.username().trim());
    }

    @DeleteMapping("/{id}/shares/{userId}")
    public ProjectDto unshare(@PathVariable String id, @PathVariable Long userId,
            Authentication authentication) {
        return service.unshare(id, authentication.getName(), userId);
    }

    @GetMapping("/{id}/shares")
    public List<String> shares(@PathVariable String id, Authentication authentication) {
        return service.listShares(id, authentication.getName());
    }
}