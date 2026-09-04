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
        TicketSuggestionDto ticketSuggestion,
        Instant createdAt
) {
    public static ChatMessageDto fromEntity(ChatMessage entity) {
        TicketSuggestionDto suggestionDto = null;
        if ("TICKET_SUGGESTED".equals(entity.getStatus())) {
            TicketSuggestion suggestion = new TicketSuggestion(
                    null,
                    entity.getTicketQuestion() != null ? entity.getTicketQuestion() : entity.getContent(),
                    null,
                    entity.getTicketUserId(),
                    entity.getTicketSector() != null ? entity.getTicketSector() : "Geral",
                    List.of(),
                    entity.getCreatedAt()
            );
            suggestionDto = TicketSuggestionDto.fromDomain(suggestion);
        }
        return new ChatMessageDto(
                entity.getId(),
                entity.getSender(),
                entity.getContent(),
                entity.getStatus(),
                entity.getSources(),
                suggestionDto,
                entity.getCreatedAt()
        );
    }
}
