package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.CardCommentDto;
import br.lcn.ragkb.dto.CardDetailDto;
import br.lcn.ragkb.dto.CardDto;
import br.lcn.ragkb.dto.CreateCardRequest;
import br.lcn.ragkb.dto.CreateCommentRequest;
import br.lcn.ragkb.dto.MoveCardRequest;
import br.lcn.ragkb.dto.SetDueDateRequest;
import br.lcn.ragkb.dto.UpdateCardRequest;
import br.lcn.ragkb.dto.UpdateCommentRequest;
import br.lcn.ragkb.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService service;

    /**
     * authentication.getName() = subject do JWT = username do AppUser
     * (mesmo padrão de UserService.getOwnProfile(username)).
     */
    @PostMapping("/api/projects/{projectId}/cards")
    public CardDto create(@PathVariable String projectId,
            @Valid @RequestBody CreateCardRequest request,
            Authentication authentication) {
        return service.create(projectId, request, authentication.getName());
    }

    @GetMapping("/api/projects/{projectId}/cards")
    public List<CardDto> board(@PathVariable String projectId,
            @RequestParam(defaultValue = "false") boolean includeClosed,
            Authentication authentication) {
        return service.board(projectId, includeClosed, authentication.getName());
    }

    @GetMapping("/api/cards/{id}")
    public CardDetailDto detail(@PathVariable String id, Authentication authentication) {
        return service.detail(id, authentication.getName());
    }

    @PutMapping("/api/cards/{id}")
    public CardDto update(@PathVariable String id,
            @Valid @RequestBody UpdateCardRequest request,
            Authentication authentication) {
        return service.update(id, request, authentication.getName());
    }

    /**
     * Prazo do card. Endpoint dedicado (não entra no UpdateCardRequest) porque
     * com record + Jackson campo ausente e null são indistinguíveis — o
     * update() apagaria o prazo a cada edição de título. dueDate null = remove.
     */
    @PatchMapping("/api/cards/{id}/due-date")
    public CardDto setDueDate(@PathVariable String id,
            @RequestBody SetDueDateRequest request,
            Authentication authentication) {
        return service.setDueDate(id, request, authentication.getName());
    }

    @PatchMapping("/api/cards/{id}/status")
    public CardDto move(@PathVariable String id,
            @Valid @RequestBody MoveCardRequest request,
            Authentication authentication) {
        return service.move(id, request, authentication.getName());
    }

    @PostMapping("/api/cards/{id}/comments")
    public CardCommentDto addComment(@PathVariable String id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication) {
        return service.addComment(id, request, authentication.getName());
    }

    @PutMapping("/api/comments/{commentId}")
    public CardCommentDto updateComment(@PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication) {
        return service.updateComment(commentId, request, authentication.getName());
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId, Authentication authentication) {
        service.deleteComment(commentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication authentication) {
        service.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}