package br.lcn.ragkb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.CardAttachment;

public interface CardAttachmentRepository extends JpaRepository<CardAttachment, String> {

    List<CardAttachment> findByCardIdOrderByUploadedAtAsc(String cardId);

    Optional<CardAttachment> findByCardIdAndId(String cardId, String id);
}
