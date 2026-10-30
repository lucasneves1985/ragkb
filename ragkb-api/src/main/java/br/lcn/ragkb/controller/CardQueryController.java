package br.lcn.ragkb.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.CardAnswerResponse;
import br.lcn.ragkb.dto.CardAskRequest;
import br.lcn.ragkb.service.CardQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Consulta em linguagem natural sobre tarefas — fluxo SEPARADO do /api/query da
 * KB por decisão de arquitetura (doc 3.7): sem roteamento entre fontes. Escopo
 * pessoal: sem @PreAuthorize — anyRequest().authenticated() cobre.
 */
@RestController
@RequiredArgsConstructor
public class CardQueryController {

    private final CardQueryService service;

    @PostMapping("/api/cards/query")
    public CardAnswerResponse ask(@Valid @RequestBody CardAskRequest request) {
        return service.ask(request);
    }
}
