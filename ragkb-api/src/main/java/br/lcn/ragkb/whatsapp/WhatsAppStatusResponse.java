package br.lcn.ragkb.whatsapp;



public record WhatsAppStatusResponse(
        boolean working,
        String status
) {}