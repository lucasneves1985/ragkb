package br.lcn.ragkb.dto;

import java.time.Instant;

public record RedisChatMessageDto(
        String sender,
        String content,
        Instant timestamp
) {
    public RedisChatMessageDto(String sender, String content) {
        this(sender, content, Instant.now());
    }
}
