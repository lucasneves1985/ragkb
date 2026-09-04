package br.lcn.ragkb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "conflict_candidates")
@Getter
@NoArgsConstructor
public class ConflictCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String documentIdA;

    @Column(nullable = false)
    private String documentIdB;

    @Column(nullable = false)
    private String chunkIdA;

    @Column(nullable = false)
    private String chunkIdB;

    @Column(nullable = false)
    private Double score;

    @Column(length = 500)
    private String snippetA;

    @Column(length = 500)
    private String snippetB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConflictStatus status = ConflictStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(length = 500)
    private String note;

    @Column
    private Instant updatedAt;

    @Column(length = 100)
    private String updatedBy;

    public ConflictCandidate(String documentIdA, String documentIdB, String chunkIdA, String chunkIdB,
                             Double score, String snippetA, String snippetB) {
        this.documentIdA = documentIdA;
        this.documentIdB = documentIdB;
        this.chunkIdA = chunkIdA;
        this.chunkIdB = chunkIdB;
        this.score = score;
        this.snippetA = snippetA;
        this.snippetB = snippetB;
        this.detectedAt = Instant.now();
    }

    public void updateStatus(ConflictStatus newStatus, String note, String updatedBy) {
        this.status = newStatus;
        this.note = note;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }
}