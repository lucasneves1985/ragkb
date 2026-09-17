package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.ChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
public record ChatMessageDto(
        Long id,
        String sender,
        String content,
        String status,
        List<String> sources,
        List<SourceReferenceDto> structuredSources,
        TicketSuggestionDto ticketSuggestion,
        Instant createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ChatMessageDto fromEntity(ChatMessage entity) {
        TicketSuggestionDto suggestionDto = null;
        if ("TICKET_SUGGESTED".equals(entity.getStatus())) {
            TicketSuggestion suggestion = new TicketSuggestion(
                    null,
                    entity.getTicketQuestion() != null ? entity.getTicketQuestion() : entity.getContent(),
                    null,
                    entity.getTicketUserId(),
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
                parseStructuredSources(entity.getSourcesJson()),
                suggestionDto,
                entity.getCreatedAt()
        );
    }

    private static List<SourceReferenceDto> parseStructuredSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<SourceReferenceDto>>() {});
        } catch (Exception e) {
            // Mensagens legadas ou JSON corrompido: fallback para os labels
            log.warn("Falha ao desserializar fontes estruturadas da mensagem", e);
            return List.of();
        }
    }
}