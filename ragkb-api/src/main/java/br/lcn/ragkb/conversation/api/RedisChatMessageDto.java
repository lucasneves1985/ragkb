package br.lcn.ragkb.conversation.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensagem compacta armazenada no histórico de conversa do Redis.
 * Campos adicionais de versões anteriores são ignorados durante a leitura.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RedisChatMessageDto(String sender, String content) {
    public RedisChatMessageDto(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }
}
