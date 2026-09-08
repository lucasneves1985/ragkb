package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectorRequest(
        @NotBlank @Size(max = 80) String name
) {}