package br.lcn.ragkb.shared.exception;



public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(String filename) {
        super("Imagem não encontrada: " + filename);
    }
}