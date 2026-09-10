package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.DocumentStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record DocumentoDto(
        String id,
        String filename,
        String sector,
        List<String> allowedSectors,
        DocumentStatus status,
        Integer chunkCount,
        Instant ingestedAt,
        List<String> allowedRoles
) {
    public static DocumentoDto from(DocumentMetadata doc) {
        return new DocumentoDto(
                doc.getId(),
                doc.getFilename(),
                doc.getSector(),
                doc.getAllowedSectors() != null ? doc.getAllowedSectors() : new ArrayList<>(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getIngestedAt(),
                doc.getAllowedRoles() != null ? doc.getAllowedRoles() : new ArrayList<>()
        );
    }
}