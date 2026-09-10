package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.ConflictCandidate;
import br.lcn.ragkb.entity.ConflictStatus;

import java.time.Instant;

public record ConflictCandidateResponse(
        Long id,
        String chunkIdA,
        String chunkIdB,
        String documentIdA,
        String documentIdB,
        Double score,
        String snippetA,
        String snippetB,
        ConflictStatus status,
        String note,
        Instant detectedAt,
        Instant updatedAt,
        String updatedBy
) {
    public static ConflictCandidateResponse from(ConflictCandidate c) {
        return new ConflictCandidateResponse(
                c.getId(),
                c.getChunkIdA(),
                c.getChunkIdB(),
                c.getDocumentIdA(),
                c.getDocumentIdB(),
                c.getScore(),
                c.getSnippetA(),
                c.getSnippetB(),
                c.getStatus(),
                c.getNote(),
                c.getDetectedAt(),
                c.getUpdatedAt(),
                c.getUpdatedBy()
        );
    }
}