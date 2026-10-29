package br.lcn.ragkb.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor
public class Reminder {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "original_request", nullable = false, columnDefinition = "TEXT")
    private String originalRequest;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "remind_at", nullable = false)
    private Instant remindAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Reminder(Long userId, String conversationId, String originalRequest,
            String summary, Instant remindAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.conversationId = conversationId;
        this.originalRequest = originalRequest;
        this.summary = summary;
        this.remindAt = remindAt;
        this.status = ReminderStatus.PENDING;
    }

    public void markSent() {
        this.status = ReminderStatus.SENT;
        this.sentAt = Instant.now();
        this.updatedAt = this.sentAt;
    }

    public void registerRetryFailure(String error) {
        this.retryCount++;
        this.lastError = error;
        this.updatedAt = Instant.now();
        if (this.retryCount >= MAX_RETRIES) {
            this.status = ReminderStatus.FAILED;
        }
    }

    public void cancel() {
        this.status = ReminderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    private static final int MAX_RETRIES = 3;

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
