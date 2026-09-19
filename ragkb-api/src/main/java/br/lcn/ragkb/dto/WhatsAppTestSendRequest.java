package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

public record WhatsAppTestSendRequest(

        @NotBlank(message = "chatId é obrigatório (formato 55DDNNNNNNNN@c.us)")
        String chatId,

        @NotBlank(message = "message é obrigatória")
        String message
) {}