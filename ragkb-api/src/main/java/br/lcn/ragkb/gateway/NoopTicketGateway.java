package br.lcn.ragkb.gateway;

import br.lcn.ragkb.dto.TicketSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoopTicketGateway implements TicketGateway {

    @Override
    public String openTicket(TicketSuggestion suggestion) {
        // TODO: Integrar com API corporativa de chamados (Jira, Zendesk, GLPI, etc.)<br/>
        log.info("Abertura de chamado solicitada: setor={}, pergunta={}, docsRelacionados={}",
                suggestion.sector(), suggestion.question(), suggestion.relatedDocumentIds());
        return "TICKET-" + suggestion.id();
    }
}