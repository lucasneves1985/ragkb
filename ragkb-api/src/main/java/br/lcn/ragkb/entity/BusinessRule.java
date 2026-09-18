package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "business_rules")
@Getter
@NoArgsConstructor
public class BusinessRule {

    @Id
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String requester;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessRuleStatus status = BusinessRuleStatus.DRAFT;

    @Column(name = "author_username", nullable = false, length = 100)
    private String authorUsername;

    @Column(name = "updated_username", length = 100)
    private String updatedUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 10)
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_rule_sectors", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "sector_name", length = 80)
    private List<String> sectorNames = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "business_rule_articles", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "article_id", length = 36)
    private List<String> articleIds = new ArrayList<>();

    public BusinessRule(String title, String description, String requester, String reason,
                        String authorUsername, List<String> sectorNames, List<String> articleIds) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.requester = requester;
        this.reason = reason;
        this.authorUsername = authorUsername;
        this.sectorNames = sectorNames != null ? new ArrayList<>(sectorNames) : new ArrayList<>();
        this.articleIds = articleIds != null ? new ArrayList<>(articleIds) : new ArrayList<>();
        this.status = BusinessRuleStatus.DRAFT;
        this.embeddingStatus = EmbeddingStatus.PENDING;
        this.version = 0;
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

    public void updateCore(String title, String description, String requester, String reason,
                           String updatedUsername, List<String> sectorNames, List<String> articleIds) {
        this.title = title;
        this.description = description;
        this.requester = requester;
        this.reason = reason;
        this.updatedUsername = updatedUsername;
        this.sectorNames = sectorNames != null ? new ArrayList<>(sectorNames) : new ArrayList<>();
        this.articleIds = articleIds != null ? new ArrayList<>(articleIds) : new ArrayList<>();
        this.version++;
    }

    public void publish() {
        this.status = BusinessRuleStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void archive() {
        this.status = BusinessRuleStatus.ARCHIVED;
    }

    public boolean isDraft() { return status == BusinessRuleStatus.DRAFT; }
    public boolean isPublished() { return status == BusinessRuleStatus.PUBLISHED; }
    public boolean isArchived() { return status == BusinessRuleStatus.ARCHIVED; }
}