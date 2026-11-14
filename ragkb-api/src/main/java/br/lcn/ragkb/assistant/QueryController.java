package br.lcn.ragkb.assistant;



import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryController {

    private final AssistantOrchestrator assistantOrchestrator;

    @PostMapping
    public AnswerResponse ask(@Valid @RequestBody AskRequest request, Authentication auth) {
        return assistantOrchestrator.ask(request.question(), request.conversationId(), auth);
    }
}
