package br.lcn.ragkb.whatsapp.api;

public class WhatsAppNotConnectedException extends RuntimeException {
    public WhatsAppNotConnectedException(String session) {
        super("Sessão WhatsApp '%s' não está conectada/emparelhada. "
                .formatted(session) + "Realize o emparelhamento com o QR Code.");
    }
}
