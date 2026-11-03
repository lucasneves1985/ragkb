package br.lcn.ragkb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor
public class Card {

    @Id
    private String id;

    /**
     * FK como String (precedente: article_id em business_rule_articles). Evita
     * associações LAZY e N+1 com open-in-view: false; a integridade referencial
     * fica no banco (fk_cards_project, ON DELETE CASCADE).
     */
    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    /**
     * Autoria: id do AppUser que criou o card (FK fk_cards_creator). Nullable
     * para cards anteriores à V18 — o lembrete de prazo os pula com warning.
     * Usado apenas para roteamento do lembrete; NÃO filtra visibilidade do
     * board (decisão pendente do usuário sobre escopo de cards).
     */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Persistido por conveniência de query; a fonte da verdade é a coluna do
     * board. Toda mudança passa por moveTo(), que grava closed_at.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CardStatus status;

    /**
     * Ordenação dentro da coluna (status).
     */
    @Column(nullable = false)
    private Integer position;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Prazo do card (apenas a data). Estado relacional: NÃO entra no índice
     * vetorial (decisão 5.1 — o vetor não lê tempo) e NÃO dispara re-embed.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Marca que o lembrete deste prazo já foi enviado (idempotência do
     * CardDueReminderJob). updateSchedule() zera o campo quando o prazo muda.
     */
    @Column(name = "due_reminder_sent_at")
    private Instant dueReminderSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Card(String projectId, Long createdBy, String title, String description, int position) {
        this.id = UUID.randomUUID().toString();
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.status = CardStatus.PENDENTE;
        this.position = position;
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

    public void updateCore(String title, String description) {
        this.title = title;
        this.description = description;
    }

    /**
     * Define/remove o prazo (null = remove). Mudar o prazo rearma o lembrete
     * (sent_at = null); salvar com o MESMO prazo não rearma — evita reenvio
     * em edições sem mudança real.
     */
    public void updateSchedule(LocalDate dueDate) {
        if (Objects.equals(this.dueDate, dueDate)) {
            return;
        }
        this.dueDate = dueDate;
        this.dueReminderSentAt = null;
    }

    public void markDueReminderSent(Instant at) {
        this.dueReminderSentAt = at;
    }

    /**
     * Transição de status. Entrar em status encerrado preenche closed_at; sair
     * (reabrir) limpa. A posição é a ordem dentro da coluna de destino.
     */
    public void moveTo(CardStatus target, int position) {
        this.status = target;
        this.position = position;
        this.closedAt = target.isClosed() ? Instant.now() : null;
    }

    /**
     * Apenas reordenação dentro da mesma coluna — NÃO usa moveTo() porque
     * moveTo() recalcula closed_at, e reordenar cards numa coluna encerrada
     * (CONCLUIDO/CANCELADO) resetaria a data de encerramento.
     */
    public void reposition(int position) {
        this.position = position;
    }
}