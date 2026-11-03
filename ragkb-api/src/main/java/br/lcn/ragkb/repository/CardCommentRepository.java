package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.CardComment;

public interface CardCommentRepository extends JpaRepository<CardComment, String> {

    List<CardComment> findByCardIdOrderByCreatedAtAsc(String cardId);
}
