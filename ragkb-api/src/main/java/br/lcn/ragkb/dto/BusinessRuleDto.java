package br.lcn.ragkb.dto;

import java.time.Instant;
import java.util.List;

import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.EmbeddingStatus;

public record BusinessRuleDto(
        String id,
        String title,
        String description,
        String requester,
        String reason,
        String status,
        List<String> sectorNames,
        List<String> articleIds,
        String authorUsername,
        String updatedUsername,
        EmbeddingStatus embeddingStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt) {

    public static BusinessRuleDto from(BusinessRule r) {
        return new BusinessRuleDto(r.getId(), r.getTitle(), r.getDescription(),
                r.getRequester(), r.getReason(), r.getStatus().name(),
                List.copyOf(r.getSectorNames()), List.copyOf(r.getArticleIds()),
                r.getAuthorUsername(), r.getUpdatedUsername(), r.getEmbeddingStatus(),
                r.getCreatedAt(), r.getUpdatedAt(), r.getPublishedAt());
    }
}