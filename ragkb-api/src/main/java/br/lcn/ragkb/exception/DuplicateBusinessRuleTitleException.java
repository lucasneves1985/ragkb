package br.lcn.ragkb.exception;

public class DuplicateBusinessRuleTitleException extends RuntimeException {
    public DuplicateBusinessRuleTitleException(String title) {
        super("Já existe uma regra com o título: " + title);
    }
}