package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.BusinessRuleDto;
import br.lcn.ragkb.dto.CreateBusinessRuleRequest;
import br.lcn.ragkb.dto.RuleActionRequest;
import br.lcn.ragkb.dto.UpdateBusinessRuleRequest;
import br.lcn.ragkb.service.BusinessRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/business-rules")
@RequiredArgsConstructor
public class BusinessRuleController {

    private final BusinessRuleService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public BusinessRuleDto create(@Valid @RequestBody CreateBusinessRuleRequest request, Authentication auth) {
        return service.create(request, auth.getName(), isAdmin(auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<BusinessRuleDto> list(Authentication auth) {
        return service.list(auth.getName(), isAdmin(auth));
    }

    @GetMapping("/portal")
    public List<BusinessRuleDto> listPortal(
            @RequestParam(required = false) String q, Authentication auth) {
        return service.listPortal(auth.getName(), isAdmin(auth), q);
    }

    @GetMapping("/{id}")
    public BusinessRuleDto getDetail(@PathVariable String id, Authentication auth) {
        return service.getDetail(id, auth.getName(), isAdmin(auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public BusinessRuleDto update(@PathVariable String id,
                                  @Valid @RequestBody UpdateBusinessRuleRequest request,
                                  Authentication auth) {
        return service.update(id, request, auth.getName(), isAdmin(auth));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public BusinessRuleDto changeStatus(@PathVariable String id,
                                        @Valid @RequestBody RuleActionRequest request,
                                        Authentication auth) {
        return switch (request.action()) {
            case "PUBLISH" -> service.publish(id, auth.getName(), isAdmin(auth));
            case "ARCHIVE" -> service.archive(id, auth.getName(), isAdmin(auth));
            default -> throw new IllegalArgumentException("Ação inválida: " + request.action());
        };
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}