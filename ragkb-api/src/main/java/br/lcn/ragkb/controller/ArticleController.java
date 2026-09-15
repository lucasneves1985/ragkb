package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.ArticleActionRequest;
import br.lcn.ragkb.dto.ArticleDto;
import br.lcn.ragkb.dto.CreateArticleRequest;
import br.lcn.ragkb.dto.UpdateArticleRequest;
import br.lcn.ragkb.service.ArticleLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleLifecycleService lifecycleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleDto create(@Valid @RequestBody CreateArticleRequest request, Authentication auth) {
        return lifecycleService.create(request, auth.getName(), isAdmin(auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<ArticleDto> list(Authentication auth) {
        return lifecycleService.list(auth.getName(), isAdmin(auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleDto update(@PathVariable String id,
                             @Valid @RequestBody UpdateArticleRequest request,
                             Authentication auth) {
        return lifecycleService.update(id, request, auth.getName(), isAdmin(auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleDto changeStatus(@PathVariable String id,
                                   @Valid @RequestBody ArticleActionRequest request,
                                   Authentication auth) {
        return switch (request.action()) {
            case "PUBLISH" -> lifecycleService.publish(id, auth.getName(), isAdmin(auth));
            case "ARCHIVE" -> lifecycleService.archive(id, auth.getName(), isAdmin(auth));
            default -> throw new IllegalArgumentException("Ação inválida: " + request.action());
        };
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}