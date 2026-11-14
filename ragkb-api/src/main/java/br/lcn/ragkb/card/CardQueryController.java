package br.lcn.ragkb.card;



import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Pergunta em linguagem natural sobre tarefas. A resposta é escopada ao
 * usuário autenticado — o CardQueryService filtra por criador.
 */
@RestController
@RequiredArgsConstructor
public class CardQueryController {

    private final CardQueryService service;

    @PostMapping("/api/cards/query")
    public CardAnswerResponse ask(@Valid @RequestBody CardAskRequest request, Authentication authentication) {
        return service.ask(request, authentication.getName());
    }
}