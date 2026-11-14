package br.lcn.ragkb.card;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardCommentRepository extends JpaRepository<CardComment, String> {

    List<CardComment> findByCardIdOrderByCreatedAtAsc(String cardId);
}
