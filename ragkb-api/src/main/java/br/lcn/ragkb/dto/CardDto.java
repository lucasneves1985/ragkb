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
        /** Username do criador (V20); null = card pré-V18 (created_by NULL). */
        String createdByUsername,
        Instant createdAt,
        Instant updatedAt) {

    public static CardDto from(Card c, String createdByUsername) {
        return new CardDto(c.getId(), c.getProjectId(), c.getTitle(), c.getDescription(),
                c.getStatus().name(), c.getPosition(), c.getClosedAt(), c.getDueDate(),
                createdByUsername, c.getCreatedAt(), c.getUpdatedAt());
    }

    /**
     * Sobrecarga legada (username null): mantida porque nem todos os chamadores
     * de CardDto.from do projeto foram revisados nesta frente. Se o grep por
     * "CardDto.from(" não encontrar chamadores fora do CardService, remova-a
     * no mesmo commit — o caminho com username resolvido é o canônico.
     */
    public static CardDto from(Card c) {
        return from(c, null);
    }
}