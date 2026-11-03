package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import lombok.RequiredArgsConstructor;

/**
 * Escopo pessoal: sem @PreAuthorize — o SecurityConfig já exige autenticação
 * para qualquer request (anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    @PostMapping
    public ProjectDto create(@Valid @RequestBody CreateProjectRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ProjectDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable String id, @Valid @RequestBody UpdateProjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
