package br.lcn.ragkb.exception;

public class DuplicateIntegrationNameException extends RuntimeException {

    public DuplicateIntegrationNameException(String name) {
        super("Já existe uma integração com o nome '%s'.".formatted(name));
    }
}