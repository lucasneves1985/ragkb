package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.CardAttachmentDto;
import br.lcn.ragkb.dto.CardCommentDto;
import br.lcn.ragkb.dto.CardDetailDto;
import br.lcn.ragkb.dto.CardDto;
import br.lcn.ragkb.dto.CreateCardRequest;
import br.lcn.ragkb.dto.CreateCommentRequest;
import br.lcn.ragkb.dto.MoveCardRequest;
import br.lcn.ragkb.dto.UpdateCardRequest;
import br.lcn.ragkb.dto.UpdateCommentRequest;
import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardComment;
import br.lcn.ragkb.entity.CardStatus;
import br.lcn.ragkb.entity.CardStatusEvent;
import br.lcn.ragkb.exception.CardCommentNotFoundException;
import br.lcn.ragkb.exception.CardNotFoundException;
import br.lcn.ragkb.exception.ProjectNotFoundException;
import br.lcn.ragkb.repository.CardAttachmentRepository;
import br.lcn.ragkb.repository.CardCommentRepository;
import br.lcn.ragkb.repository.CardRepository;
import br.lcn.ragkb.repository.CardStatusEventRepository;
import br.lcn.ragkb.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final ProjectRepository projectRepository;
    private final CardCommentRepository commentRepository;
    private final CardStatusEventRepository statusEventRepository;
    private final CardAttachmentRepository attachmentRepository;
    private final CardAttachmentService attachmentService;
    private final CardEmbeddingService embeddingService;

    @Transactional
    public CardDto create(String projectId, CreateCardRequest request) {
        ensureProjectExists(projectId);
        Card card = new Card(projectId, request.title().trim(), request.description(),
                nextPosition(projectId, CardStatus.PENDENTE));
        cardRepository.save(card);
        // Criação também é evento: from_status null -> PENDENTE.
        statusEventRepository.save(new CardStatusEvent(card.getId(), null, card.getStatus()));
        embeddingService.reembedCard(card);
        return CardDto.from(card);
    }

    /**
     * Board do projeto. Por padrão exclui encerradas (CONCLUIDO e CANCELADO); o
     * toggle "Mostrar encerradas" da UI controla includeClosed. A consulta do
     * assistente NÃO usa este filtro (decisão 3.3 da doc).
     */
    @Transactional(readOnly = true)
    public List<CardDto> board(String projectId, boolean includeClosed) {
        ensureProjectExists(projectId);
        List<Card> cards = includeClosed
                ? cardRepository.findByProjectIdOrderByPositionAsc(projectId)
                : cardRepository.findByProjectIdAndStatusNotInOrderByPositionAsc(
                        projectId, CardStatus.CLOSED_STATUSES);
        return cards.stream().map(CardDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CardDetailDto detail(String id) {
        Card card = getCard(id);
        return CardDetailDto.from(card,
                commentRepository.findByCardIdOrderByCreatedAtAsc(id),
                attachmentRepository.findByCardIdOrderByUploadedAtAsc(id),
                statusEventRepository.findByCardIdOrderByChangedAtAsc(id));
    }

    @Transactional
    public CardDto update(String id, UpdateCardRequest request) {
        Card card = getCard(id);
        card.updateCore(request.title().trim(), request.description());
        Card saved = cardRepository.save(card);
        embeddingService.reembedCard(saved);
        return CardDto.from(saved);
    }

    /**
     * Movimentação de card (drag-and-drop). Dois casos: - Mudança de coluna:
     * fecha o buraco na coluna de origem, grava o StatusEvent e insere o card
     * na coluna de destino. - Mesma coluna com position informada: reordenação
     * sem evento de status. Em ambos, a coluna de destino é renumerada 0..N-1
     * (posições contíguas). position null = fim da coluna. Mesma coluna sem
     * position = no-op. NÃO re-embute: conteúdo do card não muda; status/datas
     * entram na consulta via join com cards, não via chunk.
     */
    @Transactional
    public CardDto move(String id, MoveCardRequest request) {
        Card card = getCard(id);
        CardStatus target = parseStatus(request.status());
        boolean statusChanged = card.getStatus() != target;

        if (!statusChanged && request.position() == null) {
            return CardDto.from(card);
        }

        if (statusChanged) {
            reorderColumnWithout(card.getProjectId(), card.getStatus(), card.getId());
            statusEventRepository.save(new CardStatusEvent(card.getId(), card.getStatus(), target));
        }

        int finalPosition = insertIntoColumn(card, target, request.position());
        card.moveTo(target, finalPosition);
        return CardDto.from(cardRepository.save(card));
    }

    @Transactional
    public CardCommentDto addComment(String id, CreateCommentRequest request) {
        Card card = getCard(id);
        CardComment saved = commentRepository.save(new CardComment(id, request.content().trim()));
        embeddingService.reembedCard(card);
        return CardCommentDto.from(saved);
    }

    @Transactional
    public CardCommentDto updateComment(String commentId, UpdateCommentRequest request) {
        CardComment comment = getComment(commentId);
        comment.updateContent(request.content().trim());
        CardComment saved = commentRepository.save(comment);
        reembedCardBy(comment.getCardId());
        return CardCommentDto.from(saved);
    }

    @Transactional
    public void deleteComment(String commentId) {
        CardComment comment = getComment(commentId);
        commentRepository.delete(comment);
        reembedCardBy(comment.getCardId());
    }

    @Transactional
    public void delete(String id) {
        Card card = getCard(id);
        // Arquivos de anexo não têm cascade no disco — remover antes.
        attachmentService.deleteAllFilesForCard(id);
        cardRepository.delete(card);
    }

    private void reembedCardBy(String cardId) {
        cardRepository.findById(cardId).ifPresent(embeddingService::reembedCard);
    }

    /**
     * Insere o card na coluna de destino na posição pedida (null = fim) e
     * renumera a coluna inteira 0..N-1. Retorna a posição final do card. O
     * removeIf cobre o caso de o flush automático já ter persistido o card na
     * coluna antes da query — a lista em memória é a fonte da verdade da
     * ordenação, não o estado intermediário do banco.
     */
    private int insertIntoColumn(Card moved, CardStatus target, Integer targetPosition) {
        List<Card> column = cardRepository.findByProjectIdAndStatusOrderByPositionAsc(
                moved.getProjectId(), target);
        column.removeIf(c -> c.getId().equals(moved.getId()));
        int index = (targetPosition == null || targetPosition >= column.size())
                ? column.size()
                : Math.max(targetPosition, 0);
        column.add(index, moved);
        renumber(column);
        return index;
    }

    /**
     * Renumera 0..N-1 os cards que permanecem numa coluna após a saída de um
     * card (fecha o buraco deixado por ele).
     */
    private void reorderColumnWithout(String projectId, CardStatus status, String leavingCardId) {
        List<Card> column = cardRepository.findByProjectIdAndStatusOrderByPositionAsc(projectId, status);
        column.removeIf(c -> c.getId().equals(leavingCardId));
        renumber(column);
    }

    private void renumber(List<Card> column) {
        for (int i = 0; i < column.size(); i++) {
            Card c = column.get(i);
            if (c.getPosition() == null || c.getPosition() != i) {
                c.reposition(i);
                cardRepository.save(c);
            }
        }
    }

    private int nextPosition(String projectId, CardStatus status) {
        return cardRepository.findTopByProjectIdAndStatusOrderByPositionDesc(projectId, status)
                .map(c -> c.getPosition() + 1)
                .orElse(0);
    }

    private CardStatus parseStatus(String raw) {
        try {
            return CardStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido: " + raw);
        }
    }

    private void ensureProjectExists(String projectId) {
        projectRepository.findById(projectId).orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Card getCard(String id) {
        return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
    }

    private CardComment getComment(String commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CardCommentNotFoundException(commentId));
    }
}
