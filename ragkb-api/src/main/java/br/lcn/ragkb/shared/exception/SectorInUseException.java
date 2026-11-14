package br.lcn.ragkb.shared.exception;



public class SectorInUseException extends RuntimeException {
    public SectorInUseException(String message) {
        super(message);
    }
}