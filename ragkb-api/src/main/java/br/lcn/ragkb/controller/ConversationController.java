package br.lcn.ragkb.controller;

import br.lcn.ragkb.dto.ConversationDetailDto;
import br.lcn.ragkb.dto.ConversationSummaryDto;
import br.lcn.ragkb.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public List<ConversationSummaryDto> listConversations(Authentication auth) {
        return conversationService.listUserConversations(auth.getName());
    }

    @GetMapping("/{id}")
    public ConversationDetailDto getConversation(@PathVariable String id, Authentication auth) {
        return conversationService.getConversation(id, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDetailDto createConversation(Authentication auth) {
        return conversationService.createConversation(auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable String id, Authentication auth) {
        conversationService.deleteConversation(id, auth.getName());
    }
}
