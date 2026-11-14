package br.lcn.ragkb.whatsapp.api;

public interface WhatsAppMessagingApi {
    void sendText(String chatId, String text);
}
