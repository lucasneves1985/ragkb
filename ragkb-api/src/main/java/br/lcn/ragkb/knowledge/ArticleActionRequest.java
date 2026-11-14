package br.lcn.ragkb.knowledge;



import jakarta.validation.constraints.NotBlank;

public record ArticleActionRequest(@NotBlank String action) {}