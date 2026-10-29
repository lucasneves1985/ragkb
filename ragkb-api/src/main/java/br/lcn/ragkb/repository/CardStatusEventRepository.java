package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.CardStatusEvent;

public interface CardStatusEventRepository extends JpaRepository<CardStatusEvent, String> {

    List<CardStatusEvent> findByCardIdOrderByChangedAtAsc(String cardId);
}
