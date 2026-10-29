package br.lcn.ragkb.exception;

public class CardCommentNotFoundException extends RuntimeException {

    public CardCommentNotFoundException(String id) {
        super("Comentário não encontrado: " + id);
    }
}
