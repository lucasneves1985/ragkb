package br.lcn.ragkb.conversation;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);
    Optional<Conversation> findByIdAndUserId(String id, String userId);
}
