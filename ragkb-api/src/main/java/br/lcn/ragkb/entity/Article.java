package br.lcn.ragkb.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor
public class Article {

    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Sanitized HTML content. Images are referenced by URL pointing to
     * the media endpoint — never base64, never inline data URIs.
     * Only the plain-text extraction of this content is ingested into
     * the vector store; <img> tags are discarded during ingestion.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 80)
    private String sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Column(name = "author_username", nullable = false, length = 100)
    private String authorUsername;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_allowed_sectors", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "sector_name", length = 80)
    private List<String> allowedSectors = new ArrayList<>();

    public Article(String title, String content, String sector,
                   String authorUsername, List<String> allowedSectors) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.content = content;
        this.sector = sector;
        this.authorUsername = authorUsername;
        this.allowedSectors = allowedSectors != null
                ? new ArrayList<>(allowedSectors)
                : new ArrayList<>();
        this.status = ArticleStatus.DRAFT;
        this.chunkCount = 0;
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

    public void publish() {
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    public void archive() {
        this.status = ArticleStatus.ARCHIVED;
    }

    public void updateContent(String title, String content, List<String> allowedSectors) {
        this.title = title;
        this.content = content;
        this.allowedSectors = allowedSectors != null
                ? new ArrayList<>(allowedSectors)
                : new ArrayList<>();
        this.version++;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public boolean isDraft() {
        return status == ArticleStatus.DRAFT;
    }

    public boolean isPublished() {
        return status == ArticleStatus.PUBLISHED;
    }

    public boolean isArchived() {
        return status == ArticleStatus.ARCHIVED;
    }
}