package br.lcn.ragkb.knowledge;







import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.lcn.ragkb.identity.api.AppRole;
import br.lcn.ragkb.identity.api.SectorLookupApi;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;
    private final SectorLookupApi sectorLookupApi;

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
                .anyMatch(AppRole.ROLE_ADMIN.name()::equals);


        List<String> validatedRoles = allowedRoles.stream()
                .map(AppRole::parse)
                .map(AppRole::authority)   // metadata do PGVector mantém formato COM prefixo
                .toList();

        if (!isAdmin) {
            // Editor: forçado a ingerir do próprio setor
            String editorSector = sectorLookupApi.findSectorByUsername(auth.getName());
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
                new IngestRequest(sector, allowedSectors, validatedRoles, supersedesDocumentId), auth.getName());
    }
}
