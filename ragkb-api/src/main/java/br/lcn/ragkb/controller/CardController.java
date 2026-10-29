package br.lcn.ragkb.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import br.lcn.ragkb.dto.UpdateCardRequest;
import br.lcn.ragkb.dto.UpdateCommentRequest;
import br.lcn.ragkb.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Escopo pessoal: sem @PreAuthorize — o SecurityConfig já exige autenticação
 * para qualquer request (anyRequest().authenticated()).
 */
@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService service;

    @PostMapping("/api/projects/{projectId}/cards")
    public CardDto create(@PathVariable String projectId, @Valid @RequestBody CreateCardRequest request) {
        return service.create(projectId, request);
    }

    @GetMapping("/api/projects/{projectId}/cards")
    public List<CardDto> board(@PathVariable String projectId,
            @RequestParam(defaultValue = "false") boolean includeClosed) {
        return service.board(projectId, includeClosed);
    }

    @GetMapping("/api/cards/{id}")
    public CardDetailDto detail(@PathVariable String id) {
        return service.detail(id);
    }

    @PutMapping("/api/cards/{id}")
    public CardDto update(@PathVariable String id, @Valid @RequestBody UpdateCardRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/api/cards/{id}/status")
    public CardDto move(@PathVariable String id, @Valid @RequestBody MoveCardRequest request) {
        return service.move(id, request);
    }

    @PostMapping("/api/cards/{id}/comments")
    public CardCommentDto addComment(@PathVariable String id, @Valid @RequestBody CreateCommentRequest request) {
        return service.addComment(id, request);
    }

    @PutMapping("/api/comments/{commentId}")
    public CardCommentDto updateComment(@PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return service.updateComment(commentId, request);
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String commentId) {
        service.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/cards/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
