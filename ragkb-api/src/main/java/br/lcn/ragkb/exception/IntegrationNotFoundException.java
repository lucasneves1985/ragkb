package br.lcn.ragkb.exception;

public class IntegrationNotFoundException extends RuntimeException {

    public IntegrationNotFoundException(String id) {
        super("Integração não encontrada: " + id);
    }
}