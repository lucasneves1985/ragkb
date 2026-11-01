package br.lcn.ragkb.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardStatus;

public interface CardRepository extends JpaRepository<Card, String> {

    // ── Board (escopo por criador — cards são privados ao criador) ──
    List<Card> findByProjectIdAndCreatedByOrderByPositionAsc(String projectId, Long createdBy);

    List<Card> findByProjectIdAndCreatedByAndStatusNotInOrderByPositionAsc(
            String projectId, Long createdBy, Collection<CardStatus> statuses);

    // ── Posicionamento / reordenação (escopo por criador) ─────
    // position é por (projeto, status, criador): usuários diferentes no mesmo
    // projeto têm ordenações independentes — posições podem coincidir entre
    // criadores, mas nunca são exibidas ou renumeradas juntas.
    List<Card> findByProjectIdAndCreatedByAndStatusOrderByPositionAsc(
            String projectId, Long createdBy, CardStatus status);

    Optional<Card> findTopByProjectIdAndCreatedByAndStatusOrderByPositionDesc(
            String projectId, Long createdBy, CardStatus status);

    // ── Lembretes de prazo (CardDueReminderJob) ───────────────
    // Cards com prazo dentro da janela de alerta, fora dos status encerrados
    // (decisão 3.3 — encerradas não geram lembrete) e sem lembrete enviado.
    List<Card> findByDueDateNotNullAndDueDateLessThanEqualAndStatusNotInAndDueReminderSentAtIsNullOrderByDueDateAsc(
            LocalDate cutoff, Collection<CardStatus> statuses);

    // ── Legado do modelo single-user ──────────────────────────
    // Sem chamadores após a privacidade por criador; mantidos até confirmar
    // que nenhum outro serviço os referencia (remover em limpeza posterior).
    List<Card> findByProjectIdOrderByPositionAsc(String projectId);

    List<Card> findByProjectIdAndStatusNotInOrderByPositionAsc(
            String projectId, Collection<CardStatus> statuses);

    List<Card> findByProjectIdAndStatusOrderByPositionAsc(String projectId, CardStatus status);

    Optional<Card> findTopByProjectIdAndStatusOrderByPositionDesc(String projectId, CardStatus status);

    long countByProjectId(String projectId);
}