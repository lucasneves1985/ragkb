package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Log de transições de status — obrigatório desde o dia 1. É o que permite ao
 * assistente responder "como está/estava a tarefa X" com precisão temporal.
 * from_status é null na criação do card.
 */
@Entity
@Table(name = "card_status_events")
@Getter
@NoArgsConstructor
public class CardStatusEvent {

    @Id
    private String id;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private CardStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private CardStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    public CardStatusEvent(String cardId, CardStatus fromStatus, CardStatus toStatus) {
        this.id = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = Instant.now();
    }
}
