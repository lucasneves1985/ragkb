package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.DocumentActionRequest;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import br.lcn.ragkb.service.DocumentLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentLifecycleController {

    private final DocumentLifecycleService lifecycleService;
    private final DocumentMetadataRepository metadataRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<DocumentMetadata> listAll() {
        return metadataRepository.findAll();
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public DocumentMetadata changeStatus(@PathVariable String id,
                                         @Valid @RequestBody DocumentActionRequest request) {
        return switch (request.action()) {
            case "ARCHIVE" -> lifecycleService.archive(id);
            case "REACTIVATE" -> lifecycleService.reactivate(id, request.allowedRoles());
            default -> throw new IllegalArgumentException(
                    "Ação inválida: " + request.action() + ". Use ARCHIVE ou REACTIVATE");
        };
    }
}