package br.lcn.ragkb.exception;

public class SectorNameAlreadyExistsException extends RuntimeException {
    public SectorNameAlreadyExistsException(String name) {
        super("Já existe um setor com o nome: " + name);
    }
}