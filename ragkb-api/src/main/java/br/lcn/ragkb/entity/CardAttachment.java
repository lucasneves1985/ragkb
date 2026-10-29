package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_attachments")
@Getter
@NoArgsConstructor
public class CardAttachment {

    @Id
    private String id;

    /**
     * FK como String — padrão do projeto (ver Card.projectId).
     */
    @Column(name = "card_id", nullable = false, length = 36)
    private String cardId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    public CardAttachment(String cardId, String fileName, String contentType,
            Long sizeBytes, String storagePath) {
        this.id = UUID.randomUUID().toString();
        this.cardId = cardId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
        this.uploadedAt = Instant.now();
    }
}
