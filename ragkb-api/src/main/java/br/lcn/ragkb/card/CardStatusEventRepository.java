package br.lcn.ragkb.card;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStatusEventRepository extends JpaRepository<CardStatusEvent, String> {

    List<CardStatusEvent> findByCardIdOrderByChangedAtAsc(String cardId);
}
