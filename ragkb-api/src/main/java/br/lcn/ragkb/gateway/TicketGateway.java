package br.lcn.ragkb.gateway;

import br.lcn.ragkb.dto.TicketSuggestion;

public interface TicketGateway {
    String openTicket(TicketSuggestion suggestion);
}