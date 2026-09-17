package br.lcn.ragkb.dto;

import java.util.List;

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

    public static AnswerResponse withTicketSuggestion(String message, TicketSuggestion suggestion, String conversationId) {
        return new AnswerResponse("TICKET_SUGGESTED", message, List.of(), List.of(), TicketSuggestionDto.fromDomain(suggestion), conversationId);
    }
}