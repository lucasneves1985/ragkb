package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.ChatMessage;

import java.time.Instant;
import java.util.List;

public record ChatMessageDto(
        Long id,
        String sender,
        String content,
        String status,
        List<String> sources,
        TicketSuggestion ticketSuggestion,
        Instant createdAt
) {
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        TicketSuggestion suggestion = null;
        if ("TICKET_SUGGESTED".equals(entity.getStatus())) {
            suggestion = new TicketSuggestion(
                    null,
                    entity.getTicketQuestion() != null ? entity.getTicketQuestion() : entity.getContent(),
                    entity.getTicketUserId(),
                    entity.getTicketSector() != null ? entity.getTicketSector() : "Geral",
                    List.of(),
                    entity.getCreatedAt()
            );
        }
        return new ChatMessageDto(
                entity.getId(),
                entity.getSender(),
                entity.getContent(),
                entity.getStatus(),
                entity.getSources(),
                suggestion,
                entity.getCreatedAt()
        );
    }
}
