package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.CardComment;

public record CardCommentDto(
        String id,
        String content,
        Instant createdAt) {

    public static CardCommentDto from(CardComment c) {
        return new CardCommentDto(c.getId(), c.getContent(), c.getCreatedAt());
    }
}
