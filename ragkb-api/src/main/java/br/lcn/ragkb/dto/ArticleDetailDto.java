package br.lcn.ragkb.dto;

import java.time.Instant;
import java.util.List;

import br.lcn.ragkb.entity.Article;

public record ArticleDetailDto(
        String id,
        String title,
        String content,
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
    public static ArticleDetailDto from(Article article, String baseUrl) {
        return new ArticleDetailDto(
                article.getId(),
                article.getTitle(),
                article.getContent(),
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