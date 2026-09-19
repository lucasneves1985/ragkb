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
@Table(name = "integrations")
@Getter
@NoArgsConstructor
public class Integration {

    @Id
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private IntegrationAuthType authType = IntegrationAuthType.NONE;

    /** Criptografada via IntegrationCryptoService — nunca retornar ao cliente. */
    @Column(name = "credentials_encrypted", columnDefinition = "TEXT")
    private String credentialsEncrypted;

    @Column(name = "request_template", columnDefinition = "TEXT")
    private String requestTemplate;

    /** JSON Schema da resposta esperada. */
    @Column(name = "output_schema", columnDefinition = "TEXT")
    private String outputSchema;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 20)
    private IntegrationType integrationType;

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron;

    @Column(name = "schedule_timezone", nullable = false, length = 50)
    private String scheduleTimezone = "America/Sao_Paulo";

    @Column(name = "schedule_interval_seconds")
    private Long scheduleIntervalSeconds;

    /** Obrigatório quando QUERY — descreve à LLM o que a integração retorna. */
    @Column(name = "context_description", columnDefinition = "TEXT")
    private String contextDescription;

    /** JSON Schema dos parâmetros (coleta conversacional na frente 5). */
    @Column(name = "params_definition", columnDefinition = "TEXT")
    private String paramsDefinition;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Integration(String name, String description, String url,
                       IntegrationAuthType authType, IntegrationType integrationType,
                       String scheduleCron, String scheduleTimezone, Long scheduleIntervalSeconds,
                       String contextDescription, String requestTemplate,
                       String outputSchema, String paramsDefinition,
                       boolean active, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.url = url;
        this.authType = authType;
        this.integrationType = integrationType;
        this.scheduleCron = scheduleCron;
        if (scheduleTimezone != null && !scheduleTimezone.isBlank()) {
            this.scheduleTimezone = scheduleTimezone;
        }
        this.scheduleIntervalSeconds = scheduleIntervalSeconds;
        this.contextDescription = contextDescription;
        this.requestTemplate = requestTemplate;
        this.outputSchema = outputSchema;
        this.paramsDefinition = paramsDefinition;
        this.active = active;
        this.createdBy = createdBy;
    }

    public void assignCredentials(String encryptedCredentials) {
        this.credentialsEncrypted = encryptedCredentials;
    }

    public boolean hasCredentials() {
        return credentialsEncrypted != null && !credentialsEncrypted.isBlank();
    }

    public void updateCore(String name, String description, String url,
                           IntegrationAuthType authType, IntegrationType integrationType,
                           String scheduleCron, String scheduleTimezone, Long scheduleIntervalSeconds,
                           String contextDescription, String requestTemplate,
                           String outputSchema, String paramsDefinition, boolean active) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.authType = authType;
        this.integrationType = integrationType;
        this.scheduleCron = scheduleCron;
        if (scheduleTimezone != null && !scheduleTimezone.isBlank()) {
            this.scheduleTimezone = scheduleTimezone;
        }
        this.scheduleIntervalSeconds = scheduleIntervalSeconds;
        this.contextDescription = contextDescription;
        this.requestTemplate = requestTemplate;
        this.outputSchema = outputSchema;
        this.paramsDefinition = paramsDefinition;
        this.active = active;
        this.version++;
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