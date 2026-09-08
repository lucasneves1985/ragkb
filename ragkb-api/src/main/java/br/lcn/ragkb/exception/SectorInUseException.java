package br.lcn.ragkb.exception;

public class SectorInUseException extends RuntimeException {
    public SectorInUseException(String message) {
        super(message);
    }
}