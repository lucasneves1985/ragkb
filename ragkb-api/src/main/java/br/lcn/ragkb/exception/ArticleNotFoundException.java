package br.lcn.ragkb.exception;

public class ArticleNotFoundException extends RuntimeException {
    public ArticleNotFoundException(String id) {
        super("Artigo não encontrado: " + id);
    }
}