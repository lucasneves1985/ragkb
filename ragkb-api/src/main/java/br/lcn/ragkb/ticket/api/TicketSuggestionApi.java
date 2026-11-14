package br.lcn.ragkb.ticket.api;

public interface TicketSuggestionApi {
    SuggestionResult suggestTicket(String question, String userId, String conversationId, String reasonMessage);

    record SuggestionResult(String message, TicketSuggestionDto suggestion) {
    }
}
