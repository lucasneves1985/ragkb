package br.lcn.ragkb.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.dto.AnswerResponse;
import br.lcn.ragkb.dto.CreateTicketRequest;
import br.lcn.ragkb.dto.TicketSuggestion;
import br.lcn.ragkb.gateway.TicketGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSuggestionService {

    private final SectorClassifier sectorClassifier;
    private final TicketGateway ticketGateway;

    public AnswerResponse suggestTicket(String question, String userId, List<Document> nearMisses) {
        return suggestTicket(question, userId, nearMisses, null);
    }

    public AnswerResponse suggestTicket(String question, String userId, List<Document> nearMisses,
            String conversationId) {
        return suggestTicket(question, userId, nearMisses, conversationId, null);
    }

    /**
     * Motivo explícito: falha de integração NÃO é "informação não encontrada na
     * base" — a mensagem padrão induzia o usuário (e o desenvolvedor) a erro de
     * diagnóstico. O motivo entra como prefixo; status continua
     * TICKET_SUGGESTED para o frontend renderizar o formulário.
     */
    public AnswerResponse suggestTicket(String question, String userId, List<Document> nearMisses,
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

        return AnswerResponse.withTicketSuggestion(
                reason + " Sugiro abrir um chamado com o setor: " + sector + ".",
                suggestion,
                conversationId);
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
