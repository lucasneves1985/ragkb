package br.lcn.ragkb.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Participante de projeto compartilhado (V20). Participante tem controle
 * total sobre os cards do projeto, EXCETO exclusão — o delete permanece
 * exclusivo do criador do card (decisão do autor). O owner NÃO recebe
 * linha aqui: a participação do owner vem de projects.created_by.
 */
@Entity
@Table(name = "project_shares")
@Getter
@NoArgsConstructor
public class ProjectShare {

    @Id
    private String id;

    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shared_at", nullable = false, updatable = false)
    private Instant sharedAt;

    public ProjectShare(String projectId, Long userId) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        this.sharedAt = Instant.now();
    }
}