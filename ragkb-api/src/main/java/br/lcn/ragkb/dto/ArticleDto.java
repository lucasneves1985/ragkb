package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.Article;

import java.time.Instant;
import java.util.List;

public record ArticleDto(
        String id,
        String title,
        String sector,
        String status,
        String authorUsername,
        Integer chunkCount,
        Integer version,
        Instant createdAt,
        Instant publishedAt,
        String url,
        List<String> allowedSectors
) {
    public static ArticleDto from(Article article, String baseUrl) {
        return new ArticleDto(
                article.getId(),
                article.getTitle(),
                article.getSector(),
                article.getStatus().name(),
                article.getAuthorUsername(),
                article.getChunkCount(),
                article.getVersion(),
                article.getCreatedAt(),
                article.getPublishedAt(),
                baseUrl + "/articles/" + article.getId(),
                article.getAllowedSectors()
        );
    }
}