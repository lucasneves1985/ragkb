package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.IngestRequest;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public DocumentMetadata ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sector") String sector,
            @RequestParam(value = "allowedRoles", defaultValue = "ROLE_USER") List<String> allowedRoles,
            @RequestParam(value = "supersedesDocumentId", required = false) String supersedesDocumentId) {
        return ingestionService.ingest(file,
                new IngestRequest(sector, allowedRoles, supersedesDocumentId));
    }
}