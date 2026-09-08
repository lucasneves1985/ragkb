package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.IngestRequest;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.service.IngestionService;
import br.lcn.ragkb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final UserService userService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public DocumentMetadata ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sector") String sector,
            @RequestParam("allowedSectors") List<String> allowedSectors,
            @RequestParam(value = "allowedRoles", defaultValue = "ROLE_USER") List<String> allowedRoles,
            @RequestParam(value = "supersedesDocumentId", required = false) String supersedesDocumentId,
            Authentication auth) {

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            // Editor: forçado a ingerir do próprio setor
            String editorSector = userService.findByUsername(auth.getName());
            if (!sector.equals(editorSector)) {
                throw new IllegalArgumentException("Editor só pode ingerir documentos do seu setor: " + editorSector);
            }
            if (!allowedSectors.contains(editorSector)) {
                throw new IllegalArgumentException("allowedSectors deve incluir o setor do editor: " + editorSector);
            }
        }

        if (allowedSectors == null || allowedSectors.isEmpty()) {
            throw new IllegalArgumentException("allowedSectors é obrigatório e não pode ser vazio");
        }

        return ingestionService.ingest(file,
                new IngestRequest(sector, allowedSectors, allowedRoles, supersedesDocumentId));
    }
}