package br.lcn.ragkb.dto;

import java.time.Instant;
import java.util.List;

import br.lcn.ragkb.entity.Project;

/**
 * ownerUsername null = projeto legado (created_by NULL, anterior ao
 * compartilhamento) — visível a todos, sem gestão de compartilhamento.
 * participantUsernames lista os compartilhados (o owner não está nela — a
 * participação dele vem de created_by).
 */
public record ProjectDto(
        String id,
        String name,
        String description,
        String color,
        String ownerUsername,
        List<String> participantUsernames,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectDto from(Project p, String ownerUsername, List<String> participantUsernames) {
        return new ProjectDto(p.getId(), p.getName(), p.getDescription(), p.getColor(),
                ownerUsername, participantUsernames, p.getCreatedAt(), p.getUpdatedAt());
    }
}
