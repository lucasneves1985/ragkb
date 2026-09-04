package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.Conversation;

import java.time.Instant;

public record ConversationSummaryDto(
        String id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConversationSummaryDto fromEntity(Conversation entity) {
        return new ConversationSummaryDto(
                entity.getId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
