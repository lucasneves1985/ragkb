package br.lcn.ragkb.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardStatus;

public interface CardRepository extends JpaRepository<Card, String> {

    // ── Board LEGADO (escopo por criador — privacidade preservada) ──
    List<Card> findByProjectIdAndCreatedByOrderByPositionAsc(String projectId, Long createdBy);

    List<Card> findByProjectIdAndCreatedByAndStatusNotInOrderByPositionAsc(
            String projectId, Long createdBy, Collection<CardStatus> statuses);

    // Ordenação escopada: posições podem coincidir entre criadores, mas nunca
    // são exibidas ou renumeradas juntas (projetos legados).
    List<Card> findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
            String projectId, Long createdBy, CardStatus status);

    Optional<Card> findTopByProjectIdAndCreatedByAndStatusOrderByPositionDesc(
            String projectId, Long createdBy, CardStatus status);

    // ── Board/ordenação COMPARTILHADA (escopo global do projeto) ──
    // Cards pré-V18 (created_by NULL) permanecem invisíveis em todas as
    // superfícies (decisão 3.4) — as variantes IsNotNull os excluem do board
    // global, da renumeração e do nextPosition.
    List<Card> findByProjectIdAndCreatedByIsNotNullOrderByPositionAsc(String projectId);

    List<Card> findByProjectIdAndCreatedByIsNotNullAndStatusNotInOrderByPositionAsc(
            String projectId, Collection<CardStatus> statuses);

    List<Card> findByProjectIdAndCreatedByIsNotNullAndStatusOrderByPositionAsc(
            String projectId, CardStatus status);

    Optional<Card> findTopByProjectIdAndCreatedByIsNotNullAndStatusOrderByPositionDesc(
            String projectId, CardStatus status);

    long countByProjectId(String projectId);

    // ── Lembretes de prazo (CardDueReminderJob) ───────────────
    // Seleção do job real: cards na janela ainda não notificados.
    List<Card> findByDueDateNotNullAndDueDateLessThanEqualAndStatusNotInAndDueReminderSentAtIsNullOrderByDueDateAsc(
            LocalDate cutoff, Collection<CardStatus> statuses);

    // Teste "Checar Agora": query SEM filtro de sent_at — preview fiel do que
    // a janela contém, inclusive cards já notificados.
    List<Card> findByDueDateNotNullAndDueDateLessThanEqualAndStatusNotInOrderByDueDateAsc(
            LocalDate cutoff, Collection<CardStatus> statuses);

    // ── Digest de tarefas (CardDigestJob) — escopo compartilhado ──
    // Cards abertos com prazo até o horizonte, por criador (cards próprios).
    List<Card> findByCreatedByAndStatusNotInAndDueDateNotNullAndDueDateLessThanEqualOrderByDueDateAsc(
            Long createdBy, Collection<CardStatus> statuses, LocalDate horizon);

    // Criadores distintos com pelo menos um card aberto — base do conjunto de
    // destinatários do digest.
    @Query("select distinct c.createdBy from Card c where c.createdBy is not null and c.status not in :statuses")
    List<Long> findDistinctCreatorIdsOfOpenCards(@Param("statuses") Collection<CardStatus> statuses);

    // Owners de projetos compartilhados que têm cards abertos no horizonte —
    // o supervisor recebe digest mesmo sem criar cards (decisão V20).
    @Query("""
            SELECT DISTINCT p.createdBy FROM Project p
            WHERE p.createdBy IS NOT NULL
              AND EXISTS (SELECT c FROM Card c
                          WHERE c.projectId = p.id
                            AND c.status NOT IN :statuses
                            AND c.dueDate IS NOT NULL
                            AND c.dueDate <= :horizon)
            """)
    List<Long> findOwnerIdsWithDueCards(@Param("statuses") Collection<CardStatus> statuses,
            @Param("horizon") LocalDate horizon);

    // Participantes (shares) de projetos compartilhados com cards abertos no
    // horizonte — o analista passa a ver cards de colegas no digest.
    @Query("""
            SELECT DISTINCT s.userId FROM ProjectShare s
            WHERE EXISTS (SELECT c FROM Card c
                          WHERE c.projectId = s.projectId
                            AND c.status NOT IN :statuses
                            AND c.dueDate IS NOT NULL
                            AND c.dueDate <= :horizon)
            """)
    List<Long> findShareParticipantIdsWithDueCards(@Param("statuses") Collection<CardStatus> statuses,
            @Param("horizon") LocalDate horizon);

    // Cards com prazo acessíveis ao usuário: próprios + cards de projetos
    // compartilhados onde participa. Projetos legados NÃO ampliam o escopo
    // (p.created_by IS NOT NULL no predicado) — privacidade preservada.
    @Query("""
            SELECT c FROM Card c
            WHERE c.status NOT IN :statuses
              AND c.dueDate IS NOT NULL
              AND c.dueDate <= :horizon
              AND (c.createdBy = :userId
                   OR EXISTS (SELECT p FROM Project p
                              WHERE p.id = c.projectId
                                AND p.createdBy IS NOT NULL
                                AND (p.createdBy = :userId
                                     OR EXISTS (SELECT s FROM ProjectShare s
                                                WHERE s.projectId = p.id AND s.userId = :userId))))
            ORDER BY c.dueDate ASC
            """)
    List<Card> findDueCardsAccessibleTo(@Param("userId") Long userId,
            @Param("statuses") Collection<CardStatus> statuses, @Param("horizon") LocalDate horizon);
}