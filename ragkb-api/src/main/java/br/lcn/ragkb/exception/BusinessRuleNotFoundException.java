package br.lcn.ragkb.exception;

public class BusinessRuleNotFoundException extends RuntimeException {
    public BusinessRuleNotFoundException(String id) {
        super("Regra de negócio não encontrada: " + id);
    }
}