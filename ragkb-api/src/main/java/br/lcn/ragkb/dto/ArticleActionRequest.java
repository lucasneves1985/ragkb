package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

public record ArticleActionRequest(@NotBlank String action) {}