package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.Project;

public record ProjectDto(
        String id,
        String name,
        String description,
        String color,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectDto from(Project p) {
        return new ProjectDto(p.getId(), p.getName(), p.getDescription(),
                p.getColor(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
