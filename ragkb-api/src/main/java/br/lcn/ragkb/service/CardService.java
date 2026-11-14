package br.lcn.ragkb.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import br.lcn.ragkb.entity.Project;
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

/**
 * Serviço de cards com compartilhamento de projeto (V20). Dois modos por
 * projeto: - LEGADO (created_by NULL): comportamento anterior preservado —
 * cards privados ao criador em todas as operações; qualquer usuário cria
 * cards. - COMPARTILHADO (com owner): participantes (owner + shares) têm
 * controle total sobre os cards — criar, editar, mover, prazo, comentários e
 * anexos — EXCETO exclusão, que é do criador do card OU do owner do projeto
 * (decisão do autor; cobre o card órfão de ex-participante removido).
 * A autoria (createdBy) continua definindo o roteamento do lembrete de prazo
 * ao telefone do criador, mesmo em projeto compartilhado. Violação de acesso
 * responde 404, não 403 — não revela a existência do card alheio. Cards
 * pré-V18 (created_by NULL) permanecem invisíveis em TODAS as superfícies
 * (decisão 3.4) — inclusive no board global e para participantes/owner.
 */
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
     * Em projeto OWNED, só participantes criam (404 para não-participante);
     * em LEGADO, qualquer usuário (comportamento anterior).
     */
    @Transactional
    public CardDto create(String projectId, CreateCardRequest request, String username) {
        Long userId = currentUserId(username);
        Project project = projectRepository.findVisibleById(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        boolean shared = project.getCreatedBy() != null;
        CardStatus initialStatus = request.status() == null
                ? CardStatus.PENDENTE
                : parseStatus(request.status().name());
        Card card = new Card(projectId, userId, request.title().trim(), request.description(), 0);
        card.moveTo(initialStatus, nextPosition(projectId, userId, initialStatus, shared));
        cardRepository.save(card);
        statusEventRepository.save(new CardStatusEvent(card.getId(), null, initialStatus));
        embeddingService.reembedCard(card);
        return toDto(card);
    }

    /**
     * Board do projeto. LEGADO: escopo por criador preservado — cada usuário
     * vê apenas os cards que criou. COMPARTILHADO: board GLOBAL — participantes
     * veem todos os cards do projeto, ordenados pela position global
     * (reindexada no primeiro compartilhamento), EXCETO cards pré-V18
     * (created_by NULL), invisíveis em todas as superfícies (decisão 3.4).
     * Por padrão exclui encerradas (CONCLUIDO e CANCELADO); o toggle "Mostrar
     * encerradas" da UI controla includeClosed. A consulta do assistente NÃO
     * usa este filtro de apresentação (decisão 3.3). Cada DTO carrega o
     * username do criador (resolvido em lote — uma query para o board todo).
     */
    @Transactional(readOnly = true)
    public List<CardDto> board(String projectId, boolean includeClosed, String username) {
        Long userId = currentUserId(username);
        Project project = projectRepository.findVisibleById(projectId, userId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        List<Card> cards;
        if (project.getCreatedBy() == null) {
            cards = includeClosed
                    ? cardRepository.findByProjectIdAndCreatedByOrderByPositionAsc(projectId, userId)
                    : cardRepository.findByProjectIdAndCreatedByAndStatusNotInOrderByPositionAsc(
                            projectId, userId, CardStatus.CLOSED_STATUSES);
        } else {
            cards = includeClosed
                    ? cardRepository.findByProjectIdAndCreatedByIsNotNullOrderByPositionAsc(projectId)
                    : cardRepository.findByProjectIdAndCreatedByIsNotNullAndStatusNotInOrderByPositionAsc(
                            projectId, CardStatus.CLOSED_STATUSES);
        }
        Map<Long, String> usernamesById = usernamesFor(cards);
        return cards.stream()
                .map(c -> CardDto.from(c,
                        c.getCreatedBy() == null ? null : usernamesById.get(c.getCreatedBy())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CardDetailDto detail(String id, String username) {
        Card card = getAccessibleCard(id, currentUserId(username));
        return CardDetailDto.from(card,
                commentRepository.findByCardIdOrderByCreatedAtAsc(id),
                attachmentRepository.findByCardIdOrderByUploadedAtAsc(id),
                statusEventRepository.findByCardIdOrderByChangedAtAsc(id));
    }

    @Transactional
    public CardDto update(String id, UpdateCardRequest request, String username) {
        Card card = getAccessibleCard(id, currentUserId(username));
        card.updateCore(request.title().trim(), request.description());
        Card saved = cardRepository.save(card);
        embeddingService.reembedCard(saved);
        return toDto(saved);
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
        Card card = getAccessibleCard(id, currentUserId(username));
        card.updateSchedule(request.dueDate());
        return toDto(cardRepository.save(card));
    }

    /**
     * Movimentação de card (drag-and-drop). Dois casos: - Mudança de coluna:
     * fecha o buraco na coluna de origem, grava o StatusEvent e insere o card
     * na coluna de destino. - Mesma coluna com position informada: reordenação
     * sem evento de status. Em ambos, a coluna de destino é renumerada 0..N-1
     * (posições contíguas). LEGADO: renumeração escopada ao criador — a coluna
     * de outro usuário nunca é tocada. COMPARTILHADO: renumeração GLOBAL do
     * projeto (position única; decisão V20), excluindo cards pré-V18.
     * position null = fim da coluna. Mesma coluna sem position = no-op.
     * NÃO re-embute: conteúdo do card não muda; status/datas entram na
     * consulta via join com cards, não via chunk.
     */
    @Transactional
    public CardDto move(String id, MoveCardRequest request, String username) {
        Long userId = currentUserId(username);
        Card card = getAccessibleCard(id, userId);
        Project project = getProject(card.getProjectId());
        boolean shared = project.getCreatedBy() != null;
        CardStatus target = parseStatus(request.status());
        boolean statusChanged = card.getStatus() != target;
        if (!statusChanged && request.position() == null) {
            return toDto(card);
        }
        if (statusChanged) {
            reorderColumnWithout(card.getProjectId(), card.getStatus(), card.getId(), userId, shared);
            statusEventRepository.save(new CardStatusEvent(card.getId(), card.getStatus(), target));
        }
        int finalPosition = insertIntoColumn(card, target, request.position(), userId, shared);
        card.moveTo(target, finalPosition);
        return toDto(cardRepository.save(card));
    }

    @Transactional
    public CardCommentDto addComment(String id, CreateCommentRequest request, String username) {
        Card card = getAccessibleCard(id, currentUserId(username));
        CardComment saved = commentRepository.save(new CardComment(id, request.content().trim()));
        embeddingService.reembedCard(card);
        return CardCommentDto.from(saved);
    }

    @Transactional
    public CardCommentDto updateComment(String commentId, UpdateCommentRequest request, String username) {
        CardComment comment = getComment(commentId);
        getAccessibleCard(comment.getCardId(), currentUserId(username));
        comment.updateContent(request.content().trim());
        CardComment saved = commentRepository.save(comment);
        reembedCardBy(comment.getCardId());
        return CardCommentDto.from(saved);
    }

    @Transactional
    public void deleteComment(String commentId, String username) {
        CardComment comment = getComment(commentId);
        getAccessibleCard(comment.getCardId(), currentUserId(username));
        commentRepository.delete(comment);
        reembedCardBy(comment.getCardId());
    }

    /**
     * Exclusão do card: criador do card OU owner do projeto (decisão do autor
     * — o owner pode remover card de ex-participante). Participante comum NÃO
     * exclui card alheio. LEGADO: criador apenas.
     */
    @Transactional
    public void delete(String id, String username) {
        Card card = getDeletableCard(id, currentUserId(username));
        // Arquivos de anexo não têm cascade no disco — remover antes.
        attachmentService.deleteAllFilesForCard(id);
        cardRepository.delete(card);
    }

    /**
     * Verificação de acesso para superfícies que operam SUB-RECURSOS do card
     * (anexos): o CardAttachmentController não tem enforcement próprio e o
     * CardAttachmentService não pode depender do CardService (ciclo de
     * dependência). O controller chama este método ANTES de delegar — fonte
     * ÚNICA da regra de acesso (criador OU participante de projeto
     * compartilhado; 404 em violação). Fecha a lacuna pré-V20 em que anexos
     * de qualquer card eram operáveis por qualquer usuário autenticado.
     */
    public Card requireAccessibleCard(String id, String username) {
        return getAccessibleCard(id, currentUserId(username));
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
     * Card OPERÁVEL pelo usuário: criador, OU participante (owner ou share) de
     * projeto compartilhado. Exige o projeto visível — o criador removido do
     * compartilhamento perde o acesso ao próprio card (decisão V20: o trabalho
     * permanece no projeto, a visibilidade é dele). Cards pré-V18 (created_by
     * NULL) são invisíveis em todas as superfícies (decisão 3.4) — o guard
     * explícito impede o acesso via branch de participante. LEGADO: criador
     * apenas. Violação responde 404 (CardNotFoundException), não 403.
     */
    private Card getAccessibleCard(String id, Long userId) {
        Card card = getCard(id);
        if (card.getCreatedBy() == null) {
            throw new CardNotFoundException(id);
        }
        Project project = projectRepository.findById(card.getProjectId())
                .orElseThrow(() -> new CardNotFoundException(id));
        if (!isProjectVisible(project, userId)) {
            throw new CardNotFoundException(id);
        }
        boolean creator = card.getCreatedBy().equals(userId);
        boolean participant = project.getCreatedBy() != null
                && projectRepository.isParticipant(project.getId(), userId);
        if (creator || participant) {
            return card;
        }
        throw new CardNotFoundException(id);
    }

    /**
     * Card REMOVÍVEL: criador do card OU owner do projeto. Exige o projeto
     * visível (o owner sempre vê o próprio projeto; o criador removido do
     * compartilhamento perde o acesso — e com ele a exclusão). Cards pré-V18
     * (created_by NULL) não são removíveis por ninguém (decisão 3.4).
     * LEGADO: criador apenas.
     */
    private Card getDeletableCard(String id, Long userId) {
        Card card = getCard(id);
        if (card.getCreatedBy() == null) {
            throw new CardNotFoundException(id);
        }
        Project project = projectRepository.findById(card.getProjectId())
                .orElseThrow(() -> new CardNotFoundException(id));
        if (!isProjectVisible(project, userId)) {
            throw new CardNotFoundException(id);
        }
        boolean creator = card.getCreatedBy().equals(userId);
        boolean owner = project.getCreatedBy() != null && project.getCreatedBy().equals(userId);
        if (creator || owner) {
            return card;
        }
        throw new CardNotFoundException(id);
    }

    private boolean isProjectVisible(Project project, Long userId) {
        return project.getCreatedBy() == null
                || project.getCreatedBy().equals(userId)
                || projectRepository.isParticipant(project.getId(), userId);
    }

    // ── Hidratação do username do criador (V20) ───────────────

    /** Card único: resolve o username do criador direto (1 findById). */
    private CardDto toDto(Card card) {
        String createdByUsername = card.getCreatedBy() == null ? null
                : userRepository.findById(card.getCreatedBy())
                        .map(AppUser::getUsername).orElse(null);
        return CardDto.from(card, createdByUsername);
    }

    /**
     * Lote (board): resolve os usernames de TODOS os criadores distintos em
     * UMA query — evita N+1 de findById por card.
     */
    private Map<Long, String> usernamesFor(List<Card> cards) {
        List<Long> ids = cards.stream()
                .map(Card::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getUsername));
    }

    /**
     * Insere o card na coluna de destino na posição pedida (null = fim) e
     * renumera a coluna inteira 0..N-1. LEGADO: escopo do criador.
     * COMPARTILHADO: escopo global do projeto, excluindo cards pré-V18. O
     * removeIf cobre o caso de o flush automático já ter persistido o card na
     * coluna antes da query — a lista em memória é a fonte da verdade da
     * ordenação, não o estado intermediário do banco.
     */
    private int insertIntoColumn(Card moved, CardStatus target, Integer targetPosition,
            Long userId, boolean shared) {
        List<Card> column = shared
                ? cardRepository.findByProjectIdAndCreatedByIsNotNullAndStatusOrderByPositionAsc(
                        moved.getProjectId(), target)
                : cardRepository.findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
                        moved.getProjectId(), userId, target);
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
     * card (fecha o buraco deixado por ele). LEGADO: escopo do criador.
     * COMPARTILHADO: escopo global do projeto, excluindo cards pré-V18.
     */
    private void reorderColumnWithout(String projectId, CardStatus status, String leavingCardId,
            Long userId, boolean shared) {
        List<Card> column = shared
                ? cardRepository.findByProjectIdAndCreatedByIsNotNullAndStatusOrderByPositionAsc(
                        projectId, status)
                : cardRepository.findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
                        projectId, userId, status);
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

    private int nextPosition(String projectId, Long userId, CardStatus status, boolean shared) {
        if (shared) {
            return cardRepository
                    .findTopByProjectIdAndCreatedByIsNotNullAndStatusOrderByPositionDesc(projectId, status)
                    .map(c -> c.getPosition() + 1)
                    .orElse(0);
        }
        return cardRepository
                .findTopByProjectIdAndCreatedByAndStatusOrderByPositionDesc(projectId, userId, status)
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

    private Project getProject(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Card getCard(String id) {
        return cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException(id));
    }

    private CardComment getComment(String commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CardCommentNotFoundException(commentId));
    }
}