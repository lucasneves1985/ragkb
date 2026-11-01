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
import br.lcn.ragkb.dto.SetDueDateRequest;
import br.lcn.ragkb.dto.UpdateCardRequest;
import br.lcn.ragkb.dto.UpdateCommentRequest;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardComment;
import br.lcn.ragkb.entity.CardStatus;
import br.lcn.ragkb.entity.CardStatusEvent;
import br.lcn.ragkb.exception.CardCommentNotFoundException;
import br.lcn.ragkb.exception.CardNotFoundException;
import br.lcn.ragkb.exception.ProjectNotFoundException;
import br.lcn.ragkb.repository.AppUserRepository;
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
    private final AppUserRepository userRepository;
    private final CardCommentRepository commentRepository;
    private final CardStatusEventRepository statusEventRepository;
    private final CardAttachmentRepository attachmentRepository;
    private final CardAttachmentService attachmentService;
    private final CardEmbeddingService embeddingService;

    /**
     * Criação estilo Trello: status inicial opcional (null = PENDENTE). O
     * evento de criação é null -> status informado; NUNCA uma transição
     * PENDENTE -> alvo, que poluiria o histórico consumido pelo assistente.
     * A autoria (createdBy) define a privacidade: só o criador vê e opera o
     * card, e é ela que roteia o lembrete de prazo ao telefone do cadastro.
     */
    @Transactional
    public CardDto create(String projectId, CreateCardRequest request, String username) {
        ensureProjectExists(projectId);
        Long creatorId = currentUserId(username);
        CardStatus initialStatus = request.status() == null
                ? CardStatus.PENDENTE
                : parseStatus(request.status().name());
        Card card = new Card(projectId, creatorId, request.title().trim(), request.description(), 0);
        card.moveTo(initialStatus, nextPosition(projectId, creatorId, initialStatus));
        cardRepository.save(card);
        statusEventRepository.save(new CardStatusEvent(card.getId(), null, initialStatus));
        embeddingService.reembedCard(card);
        return CardDto.from(card);
    }

    /**
     * Board do projeto, ESCOPADO AO CRIADOR — cada usuário vê apenas os cards
     * que criou (decisão de privacidade; compartilhamento de projeto é
     * evolução futura). Por padrão exclui encerradas (CONCLUIDO e CANCELADO);
     * o toggle "Mostrar encerradas" da UI controla includeClosed. A consulta
     * do assistente NÃO usa este filtro de apresentação (decisão 3.3).
     */
    @Transactional(readOnly = true)
    public List<CardDto> board(String projectId, boolean includeClosed, String username) {
        ensureProjectExists(projectId);
        Long creatorId = currentUserId(username);
        List<Card> cards = includeClosed
                ? cardRepository.findByProjectIdAndCreatedByOrderByPositionAsc(projectId, creatorId)
                : cardRepository.findByProjectIdAndCreatedByAndStatusNotInOrderByPositionAsc(
                        projectId, creatorId, CardStatus.CLOSED_STATUSES);
        return cards.stream().map(CardDto::from).toList();
    }

    @Transactional(readOnly = true)
    public CardDetailDto detail(String id, String username) {
        Card card = getOwnedCard(id, currentUserId(username));
        return CardDetailDto.from(card,
                commentRepository.findByCardIdOrderByCreatedAtAsc(id),
                attachmentRepository.findByCardIdOrderByUploadedAtAsc(id),
                statusEventRepository.findByCardIdOrderByChangedAtAsc(id));
    }

    @Transactional
    public CardDto update(String id, UpdateCardRequest request, String username) {
        Card card = getOwnedCard(id, currentUserId(username));
        card.updateCore(request.title().trim(), request.description());
        Card saved = cardRepository.save(card);
        embeddingService.reembedCard(saved);
        return CardDto.from(saved);
    }

    /**
     * Define/remove o prazo (dueDate null = remove) via endpoint dedicado.
     * NÃO re-embute: due_date é estado relacional (decisão 5.1) — mudar o
     * prazo não altera o conteúdo do card. updateSchedule() rearma o lembrete
     * (zera due_reminder_sent_at) apenas quando o prazo muda de fato; salvar
     * o mesmo prazo é no-op, evitando reenvio em edições repetidas.
     */
    @Transactional
    public CardDto setDueDate(String id, SetDueDateRequest request, String username) {
        Card card = getOwnedCard(id, currentUserId(username));
        card.updateSchedule(request.dueDate());
        return CardDto.from(cardRepository.save(card));
    }

    /**
     * Movimentação de card (drag-and-drop). Dois casos: - Mudança de coluna:
     * fecha o buraco na coluna de origem, grava o StatusEvent e insere o card
     * na coluna de destino. - Mesma coluna com position informada: reordenação
     * sem evento de status. Em ambos, a coluna de destino é renumerada 0..N-1
     * (posições contíguas) DENTRO DO ESCOPO DO CRIADOR — a coluna de outro
     * usuário nunca é tocada. position null = fim da coluna. Mesma coluna sem
     * position = no-op. NÃO re-embute: conteúdo do card não muda; status/datas
     * entram na consulta via join com cards, não via chunk.
     */
    @Transactional
    public CardDto move(String id, MoveCardRequest request, String username) {
        Long creatorId = currentUserId(username);
        Card card = getOwnedCard(id, creatorId);
        CardStatus target = parseStatus(request.status());
        boolean statusChanged = card.getStatus() != target;

        if (!statusChanged && request.position() == null) {
            return CardDto.from(card);
        }

        if (statusChanged) {
            reorderColumnWithout(card.getProjectId(), card.getStatus(), card.getId(), creatorId);
            statusEventRepository.save(new CardStatusEvent(card.getId(), card.getStatus(), target));
        }

        int finalPosition = insertIntoColumn(card, target, request.position(), creatorId);
        card.moveTo(target, finalPosition);
        return CardDto.from(cardRepository.save(card));
    }

    @Transactional
    public CardCommentDto addComment(String id, CreateCommentRequest request, String username) {
        Card card = getOwnedCard(id, currentUserId(username));
        CardComment saved = commentRepository.save(new CardComment(id, request.content().trim()));
        embeddingService.reembedCard(card);
        return CardCommentDto.from(saved);
    }

    @Transactional
    public CardCommentDto updateComment(String commentId, UpdateCommentRequest request, String username) {
        CardComment comment = getComment(commentId);
        getOwnedCard(comment.getCardId(), currentUserId(username));
        comment.updateContent(request.content().trim());
        CardComment saved = commentRepository.save(comment);
        reembedCardBy(comment.getCardId());
        return CardCommentDto.from(saved);
    }

    @Transactional
    public void deleteComment(String commentId, String username) {
        CardComment comment = getComment(commentId);
        getOwnedCard(comment.getCardId(), currentUserId(username));
        commentRepository.delete(comment);
        reembedCardBy(comment.getCardId());
    }

    @Transactional
    public void delete(String id, String username) {
        Card card = getOwnedCard(id, currentUserId(username));
        // Arquivos de anexo não têm cascade no disco — remover antes.
        attachmentService.deleteAllFilesForCard(id);
        cardRepository.delete(card);
    }

    private void reembedCardBy(String cardId) {
        cardRepository.findById(cardId).ifPresent(embeddingService::reembedCard);
    }

    /**
     * Resolve o id do AppUser autenticado a partir do username (subject do
     * JWT). Padrão do projeto: username como parâmetro (UserService).
     */
    private Long currentUserId(String username) {
        return userRepository.findByUsername(username)
                .map(AppUser::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário autenticado não encontrado: " + username));
    }

    /**
     * Busca o card e verifica a autoria. Violação de posse responde 404
     * (CardNotFoundException), não 403 — não revela a existência do card de
     * outro usuário. Cards sem created_by (anteriores à V18) são inacessíveis
     * a todos.
     */
    private Card getOwnedCard(String id, Long creatorId) {
        Card card = getCard(id);
        if (card.getCreatedBy() == null || !card.getCreatedBy().equals(creatorId)) {
            throw new CardNotFoundException(id);
        }
        return card;
    }

    /**
     * Insere o card na coluna de destino na posição pedida (null = fim) e
     * renumera a coluna inteira 0..N-1 no escopo do criador. Retorna a posição
     * final do card. O removeIf cobre o caso de o flush automático já ter
     * persistido o card na coluna antes da query — a lista em memória é a
     * fonte da verdade da ordenação, não o estado intermediário do banco.
     */
    private int insertIntoColumn(Card moved, CardStatus target, Integer targetPosition, Long creatorId) {
        List<Card> column = cardRepository.findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
                moved.getProjectId(), creatorId, target);
        column.removeIf(c -> c.getId().equals(moved.getId()));
        int index = (targetPosition == null || targetPosition >= column.size())
                ? column.size()
                : Math.max(targetPosition, 0);
        column.add(index, moved);
        renumber(column);
        return index;
    }

    /**
     * Renumera 0..N-1 os cards DO CRIADOR que permanecem numa coluna após a
     * saída de um card (fecha o buraco deixado por ele).
     */
    private void reorderColumnWithout(String projectId, CardStatus status, String leavingCardId, Long creatorId) {
        List<Card> column = cardRepository.findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
                projectId, creatorId, status);
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

    private int nextPosition(String projectId, Long creatorId, CardStatus status) {
        return cardRepository
                .findTopByProjectIdAndCreatedByAndStatusOrderByPositionDesc(projectId, creatorId, status)
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