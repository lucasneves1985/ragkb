package br.lcn.ragkb.shared.exception;







public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String id) {
        super("Card não encontrado: " + id);
    }
}
