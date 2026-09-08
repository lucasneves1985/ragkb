package br.lcn.ragkb.exception;

public class SectorNotFoundException extends RuntimeException {
    public SectorNotFoundException(Long id) {
        super("Setor não encontrado: " + id);
    }
}