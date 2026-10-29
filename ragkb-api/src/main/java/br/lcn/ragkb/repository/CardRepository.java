package br.lcn.ragkb.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardStatus;

public interface CardRepository extends JpaRepository<Card, String> {

    // ── Board ─────────────────────────────────────────────────
    List<Card> findByProjectIdOrderByPositionAsc(String projectId);

    List<Card> findByProjectIdAndStatusNotInOrderByPositionAsc(
            String projectId, Collection<CardStatus> statuses);

    // ── Posicionamento / reordenação ──────────────────────────
    List<Card> findByProjectIdAndStatusOrderByPositionAsc(String projectId, CardStatus status);

    Optional<Card> findTopByProjectIdAndStatusOrderByPositionDesc(String projectId, CardStatus status);

    long countByProjectId(String projectId);
}
