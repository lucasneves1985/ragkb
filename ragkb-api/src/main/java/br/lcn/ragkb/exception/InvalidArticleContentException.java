package br.lcn.ragkb.exception;

public class InvalidArticleContentException extends RuntimeException {
    public InvalidArticleContentException(String message) {
        super(message);
    }
}