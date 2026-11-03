package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        String description,
        @Size(max = 7)
        String color) {

}
