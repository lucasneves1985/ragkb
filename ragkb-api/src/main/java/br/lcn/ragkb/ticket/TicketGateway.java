package br.lcn.ragkb.ticket;



public interface TicketGateway {
    String openTicket(TicketSuggestion suggestion);
}