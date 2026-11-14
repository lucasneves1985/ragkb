package br.lcn.ragkb.assistant;







import java.util.List;

import br.lcn.ragkb.ticket.api.TicketSuggestionDto;

public record AnswerResponse(
        String status,
        String answer,
        List<String> sourceIds,
        List<SourceReferenceDto> sources,
        TicketSuggestionDto ticketSuggestion,
        String conversationId
        ) {

    public static AnswerResponse fromKnowledgeBase(String answer, List<String> sourceIds,
            List<SourceReferenceDto> sources, String conversationId) {
        return new AnswerResponse("KNOWLEDGE", answer, sourceIds, sources, null, conversationId);
    }

    public static AnswerResponse withTicketSuggestion(
            String message, TicketSuggestionDto suggestion, String conversationId) {
        return new AnswerResponse("TICKET_SUGGESTED", message, List.of(), List.of(),
                suggestion, conversationId);
    }

    /**
     * Fluxo não-KB do lembrete (frente 4): confirmação de agendamento.
     */
    public static AnswerResponse fromReminder(String message, String conversationId) {
        return new AnswerResponse("REMINDER_CREATED", message, List.of(), List.of(),
                null, conversationId);
    }
}
