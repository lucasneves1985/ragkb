package br.lcn.ragkb.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String id) {
        super("Projeto não encontrado: " + id);
    }
}
