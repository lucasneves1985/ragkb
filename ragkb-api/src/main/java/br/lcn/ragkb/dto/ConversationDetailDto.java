package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.Conversation;

import java.time.Instant;
import java.util.List;

public record ConversationDetailDto(
        String id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<ChatMessageDto> messages
) {
    public static ConversationDetailDto fromEntity(Conversation entity) {
        List<ChatMessageDto> msgDtos = entity.getMessages().stream()
                .map(ChatMessageDto::fromEntity)
                .toList();
        return new ConversationDetailDto(
                entity.getId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                msgDtos
        );
    }
}
