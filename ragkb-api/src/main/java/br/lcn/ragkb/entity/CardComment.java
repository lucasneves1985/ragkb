package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_comments")
@Getter
@NoArgsConstructor
public class CardComment {

    @Id
    private String id;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CardComment(String cardId, String content) {
        this.id = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
