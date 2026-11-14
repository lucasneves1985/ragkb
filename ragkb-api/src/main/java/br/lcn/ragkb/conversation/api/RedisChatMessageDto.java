package br.lcn.ragkb.conversation.api;

public record RedisChatMessageDto(String sender, String content) {
    public RedisChatMessageDto(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }
}
