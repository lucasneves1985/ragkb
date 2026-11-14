package br.lcn.ragkb.ticket;







import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.identity.api.SectorClassificationApi;
import br.lcn.ragkb.ticket.api.TicketSuggestionApi;
import br.lcn.ragkb.ticket.api.TicketSuggestionDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSuggestionService implements TicketSuggestionApi {

    public record SuggestionResult(String message, TicketSuggestion suggestion) {
    }

    private final SectorClassificationApi sectorClassifier;
    private final TicketGateway ticketGateway;

    public SuggestionResult suggestTicket(String question, String userId, List<Document> nearMisses) {
        return suggestTicket(question, userId, nearMisses, null);
    }

    public SuggestionResult suggestTicket(String question, String userId, List<Document> nearMisses,
            String conversationId) {
        return suggestTicket(question, userId, nearMisses, conversationId, null);
    }

    /**
     * Motivo explícito: falha de integração NÃO é "informação não encontrada na
     * base" — a mensagem padrão induzia o usuário (e o desenvolvedor) a erro de
     * diagnóstico. O motivo entra como prefixo; status continua
     * TICKET_SUGGESTED para o frontend renderizar o formulário.
     */
    public SuggestionResult suggestTicket(String question, String userId, List<Document> nearMisses,
            String conversationId, String reasonMessage) {
        String sector = nearMisses.isEmpty()
                ? sectorClassifier.classify(question)
                : nearMisses.get(0).getMetadata().get("sector").toString();

        TicketSuggestion suggestion = new TicketSuggestion(
                UUID.randomUUID(),
                question,
                "",
                userId,
                nearMisses.stream()
                        .map(doc -> doc.getMetadata().get("documentId").toString())
                        .toList(),
                Instant.now());

        String reason = reasonMessage != null && !reasonMessage.isBlank()
                ? reasonMessage
                : "Não encontrei essa informação na base de conhecimento.";

        return new SuggestionResult(
                reason + " Sugiro abrir um chamado com o setor: " + sector + ".", suggestion);
    }

    @Override
    public TicketSuggestionApi.SuggestionResult suggestTicket(
            String question, String userId, String conversationId, String reasonMessage) {
        SuggestionResult result = suggestTicket(question, userId, List.of(), conversationId, reasonMessage);
        return new TicketSuggestionApi.SuggestionResult(
                result.message(), toDto(result.suggestion()));
    }

    private TicketSuggestionDto toDto(TicketSuggestion suggestion) {
        return new TicketSuggestionDto(suggestion.question(), suggestion.description(), suggestion.userId());
    }

    public String confirmAndOpen(TicketSuggestion suggestion) {
        return ticketGateway.openTicket(suggestion);
    }

    public String createFromRequest(CreateTicketRequest request) {
        TicketSuggestion suggestion = new TicketSuggestion(
                UUID.randomUUID(),
                request.subject(),
                request.description(),
                request.requester(),
                List.of(),
                Instant.now());
        return ticketGateway.openTicket(suggestion);
    }
}
