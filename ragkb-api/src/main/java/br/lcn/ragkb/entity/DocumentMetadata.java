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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor
public class DocumentMetadata {

    @Id
    private String id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String sector;

    @Column(nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private Integer chunkCount;

    @Column(nullable = false, updatable = false)
    private Instant ingestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "supersedes_document_id")
    private String supersedesDocumentId;

    @Column(columnDefinition = "TEXT")
    private String sourceText;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_allowed_roles", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role", length = 50)
    private List<String> allowedRoles = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_allowed_sectors", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "sector_name", length = 80)
    private List<String> allowedSectors = new ArrayList<>();

    public DocumentMetadata(String id, String filename, String sector, String contentHash,
                            Integer chunkCount, String supersedesDocumentId,
                            String sourceText, List<String> allowedRoles, List<String> allowedSectors) {
        this.id = id;
        this.filename = filename;
        this.sector = sector;
        this.contentHash = contentHash;
        this.chunkCount = chunkCount;
        this.ingestedAt = Instant.now();
        this.status = DocumentStatus.ACTIVE;
        this.supersedesDocumentId = supersedesDocumentId;
        this.sourceText = sourceText;
        this.allowedRoles = allowedRoles != null ? new ArrayList<>(allowedRoles) : new ArrayList<>();
        this.allowedSectors = allowedSectors != null ? new ArrayList<>(allowedSectors) : new ArrayList<>();
    }

    public void supersede() {
        this.status = DocumentStatus.SUPERSEDED;
    }

    public void archive() {
        this.status = DocumentStatus.ARCHIVED;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public void setAllowedRoles(List<String> roles) {
        this.allowedRoles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }
}