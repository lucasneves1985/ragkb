package br.lcn.ragkb.exception;

public class DuplicateProjectNameException extends RuntimeException {

    public DuplicateProjectNameException(String name) {
        super("Já existe um projeto com este nome: " + name);
    }
}
