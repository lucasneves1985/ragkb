package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Chunk vetorial do card. A coluna embedding (vector) NÃO é mapeada no
 * Hibernate (ddl-auto: validate) — acesso só via native query, mesmo padrão do
 * BusinessRuleRepository.
 */
@Entity
@Table(name = "card_chunks")
@Getter
@NoArgsConstructor
public class CardChunk {

    @Id
    private String id;

    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    /**
     * Denormalizado para filtro por projeto na busca semântica.
     */
    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false, length = 10)
    private CardChunkType chunkType;

    /**
     * Id do comentário quando chunkType = COMMENT; null para CARD.
     */
    @Column(name = "source_id", length = 36)
    private String sourceId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 10)
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CardChunk(String cardId, String projectId, CardChunkType chunkType,
            String sourceId, int chunkIndex, String content) {
        this.id = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.projectId = projectId;
        this.chunkType = chunkType;
        this.sourceId = sourceId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embeddingStatus = EmbeddingStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
