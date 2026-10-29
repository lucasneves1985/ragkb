package br.lcn.ragkb.dto;

import java.time.Instant;
import java.util.List;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardAttachment;
import br.lcn.ragkb.entity.CardComment;
import br.lcn.ragkb.entity.CardStatusEvent;

public record CardDetailDto(
        String id,
        String projectId,
        String title,
        String description,
        String status,
        Integer position,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        List<CardCommentDto> comments,
        List<CardAttachmentDto> attachments,
        List<CardStatusEventDto> statusHistory) {

    public static CardDetailDto from(Card c, List<CardComment> comments,
            List<CardAttachment> attachments,
            List<CardStatusEvent> events) {
        return new CardDetailDto(c.getId(), c.getProjectId(), c.getTitle(), c.getDescription(),
                c.getStatus().name(), c.getPosition(), c.getClosedAt(),
                c.getCreatedAt(), c.getUpdatedAt(),
                comments.stream().map(CardCommentDto::from).toList(),
                attachments.stream().map(CardAttachmentDto::from).toList(),
                events.stream().map(CardStatusEventDto::from).toList());
    }
}
