package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.CreateTicketRequest;
import br.lcn.ragkb.dto.TicketSuggestion;
import br.lcn.ragkb.service.TicketSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketSuggestionService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CreateTicketRequest request) {
        String ticketId = ticketService.createFromRequest(request);
        return Map.of("ticketId", ticketId, "message", "Chamado criado com sucesso.");
    }

    @PostMapping("/confirm")
    public String confirm(@RequestBody TicketSuggestion suggestion) {
        return ticketService.confirmAndOpen(suggestion);
    }
}