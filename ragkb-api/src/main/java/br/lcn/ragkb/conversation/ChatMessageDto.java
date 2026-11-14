package br.lcn.ragkb.conversation;







import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ChatMessageDto(
        Long id,
        String sender,
        String content,
        String status,
        List<String> sources,
        List<ConversationSourceReference> structuredSources,
        ConversationTicketSuggestion ticketSuggestion,
        Instant createdAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ChatMessageDto fromEntity(ChatMessage entity) {
        ConversationTicketSuggestion suggestionDto = null;
        if ("TICKET_SUGGESTED".equals(entity.getStatus())) {
            String ticketQuestion = entity.getTicketQuestion() != null
                    ? entity.getTicketQuestion() : entity.getContent();
            suggestionDto = new ConversationTicketSuggestion(ticketQuestion, null, entity.getTicketUserId());
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

    private static List<ConversationSourceReference> parseStructuredSources(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<ConversationSourceReference>>() {});
        } catch (Exception e) {
            // Mensagens legadas ou JSON corrompido: fallback para os labels
            log.warn("Falha ao desserializar fontes estruturadas da mensagem", e);
            return List.of();
        }
    }
}
