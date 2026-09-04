package br.lcn.ragkb.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketSuggestion(
        UUID id,
        String question,
        String userId,
        String sector,
        List<String> relatedDocumentIds,
        Instant createdAt
) {}