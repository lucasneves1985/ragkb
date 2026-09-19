package br.lcn.ragkb.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "integration_executions")
@Getter
@NoArgsConstructor
public class IntegrationExecution {

    @Id
    private String id;

    @Column(name = "integration_id", nullable = false, length = 36)
    private String integrationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntegrationExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Setter
    @Column(name = "finished_at")
    private Instant finishedAt;

    @Setter
    @Column(nullable = false)
    private Integer attempt = 1;

    @Setter
    @Column(name = "http_status")
    private Integer httpStatus;

    @Setter
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Setter
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public IntegrationExecution(String integrationId) {
        this.id = UUID.randomUUID().toString();
        this.integrationId = integrationId;
        this.status = IntegrationExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void success(Integer httpStatus, String responseBody) {
        this.status = IntegrationExecutionStatus.SUCCESS;
        this.httpStatus = httpStatus;
        this.responseBody = truncate(responseBody);
        this.finishedAt = Instant.now();
    }

    public void failure(Integer httpStatus, String responseBody, String errorMessage) {
        this.status = IntegrationExecutionStatus.FAILED;
        this.httpStatus = httpStatus;
        this.responseBody = truncate(responseBody);
        this.errorMessage = truncate(errorMessage);
        this.finishedAt = Instant.now();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 5000) {
            return value;
        }
        return value.substring(0, 5000);
    }

    @PrePersist
    void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }
}
