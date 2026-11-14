package br.lcn.ragkb.ticket.api;

public record TicketSuggestionDto(
        String subject,
        String description,
        String requester) {
}
