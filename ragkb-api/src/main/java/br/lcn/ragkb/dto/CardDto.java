package br.lcn.ragkb.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.lcn.ragkb.entity.Card;

public record CardDto(
        String id,
        String projectId,
        String title,
        String description,
        String status,
        Integer position,
        Instant closedAt,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static CardDto from(Card c) {
        return new CardDto(c.getId(), c.getProjectId(), c.getTitle(), c.getDescription(),
                c.getStatus().name(), c.getPosition(), c.getClosedAt(), c.getDueDate(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}