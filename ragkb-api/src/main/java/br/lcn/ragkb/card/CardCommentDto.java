package br.lcn.ragkb.card;



import java.time.Instant;

public record CardCommentDto(
        String id,
        String content,
        Instant createdAt) {

    public static CardCommentDto from(CardComment c) {
        return new CardCommentDto(c.getId(), c.getContent(), c.getCreatedAt());
    }
}
