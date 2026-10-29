package br.lcn.ragkb.whatsapp;

public class WhatsAppNotConnectedException extends RuntimeException {

    public WhatsAppNotConnectedException(String session) {
        super("Sessão WhatsApp '%s' não está conectada/emparelhada. "
                + "Realize o emparelhamento com o QR Code.".formatted(session));
    }
}