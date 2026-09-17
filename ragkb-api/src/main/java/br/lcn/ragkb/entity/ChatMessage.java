package br.lcn.ragkb.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(nullable = false, length = 20)
    private String sender; // "USER" or "ASSISTANT"

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 30)
    private String status; // "KNOWLEDGE" or "TICKET_SUGGESTED"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message_sources", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "source", length = 255)
    private List<String> sources = new ArrayList<>();

    /**
     * Structured sources as JSON (List of SourceReferenceDto) — populated
     * on new messages so the history reload can render article links.
     * Null on messages persisted before this column existed.
     */
    @Column(name = "sources_json", columnDefinition = "TEXT")
    private String sourcesJson;

    private String ticketSector;
    private String ticketQuestion;
    private String ticketUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage(Conversation conversation, String sender, String content, String status, List<String> sources) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.status = status;
        if (sources != null) {
            this.sources = new ArrayList<>(sources);
        }
        this.createdAt = Instant.now();
    }
}