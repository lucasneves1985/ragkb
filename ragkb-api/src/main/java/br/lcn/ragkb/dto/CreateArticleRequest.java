package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        String sector,
        @NotEmpty List<String> allowedSectors
) {}