package br.lcn.ragkb.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    public Conversation(String userId, String title) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.title = title != null ? title : "Nova conversa";
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addMessage(ChatMessage message) {
        message.setConversation(this);
        this.messages.add(message);
        this.updatedAt = Instant.now();
    }
}
