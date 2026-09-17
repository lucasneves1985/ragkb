package br.lcn.ragkb.dto;

public record SourceReferenceDto(
        String type,   // DOCUMENT | ARTICLE
        String label,
        String url     // null para documentos
) {
    public static SourceReferenceDto fromDocument(String filename) {
        return new SourceReferenceDto("DOCUMENT", filename, null);
    }

    public static SourceReferenceDto fromArticle(String title, String url) {
        return new SourceReferenceDto("ARTICLE", title, url);
    }
}