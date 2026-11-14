package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentLifecycleController {

    private final DocumentLifecycleService lifecycleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<DocumentDto> listAll() {
        return lifecycleService.listAll();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public DocumentDto changeStatus(@PathVariable String id,
                                    @Valid @RequestBody DocumentActionRequest request,
                                    Authentication auth) {
        return switch (request.action()) {
            case "ARCHIVE" -> lifecycleService.archive(id, auth.getName());
            case "REACTIVATE" -> lifecycleService.reactivate(id, request.allowedRoles(), auth.getName());
            default -> throw new IllegalArgumentException(
                    "Ação inválida: " + request.action() + ". Use ARCHIVE ou REACTIVATE");
        };
    }

    @PatchMapping("/{id}/access")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public DocumentDto updateAccess(@PathVariable String id,
                                    @Valid @RequestBody UpdateDocumentAccessRequest request) {
        return lifecycleService.updateAllowedSectors(id, request.allowedSectors());
    }
}
