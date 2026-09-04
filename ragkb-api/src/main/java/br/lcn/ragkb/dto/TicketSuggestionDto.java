package br.lcn.ragkb.dto;

public record TicketSuggestionDto(
        String subject,
        String description,
        String requester,
        String sector
) {
    public static TicketSuggestionDto fromDomain(TicketSuggestion s) {
        return new TicketSuggestionDto(s.question(), s.description(), s.userId(), s.sector());
    }
}