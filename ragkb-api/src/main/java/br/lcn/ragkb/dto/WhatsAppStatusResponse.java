package br.lcn.ragkb.dto;

public record WhatsAppStatusResponse(
        boolean working,
        String status
) {}