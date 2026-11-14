package br.lcn.ragkb.shared.exception;



public class CardCommentNotFoundException extends RuntimeException {

    public CardCommentNotFoundException(String id) {
        super("Comentário não encontrado: " + id);
    }
}
