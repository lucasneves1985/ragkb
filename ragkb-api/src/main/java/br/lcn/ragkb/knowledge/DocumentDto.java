package br.lcn.ragkb.knowledge;



import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record DocumentDto(
        String id,
        String filename,
        String sector,
        List<String> allowedSectors,
        DocumentStatus status,
        Integer chunkCount,
        Instant ingestedAt,
        List<String> allowedRoles
) {
    public static DocumentDto from(DocumentMetadata doc) {
        return new DocumentDto(
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