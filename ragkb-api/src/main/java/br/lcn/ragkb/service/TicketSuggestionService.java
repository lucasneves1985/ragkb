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

    public AnswerResponse suggestTicket(String question, String userId, List<Document> nearMisses, String conversationId) {
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

        // Retorna a sugestão sem auto-abertura imediata (requer confirmação do usuário)
        return AnswerResponse.withTicketSuggestion(
                "Não encontrei essa informação na base de conhecimento. "
                        + "Sugiro abrir um chamado com o setor: " + sector + ".",
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