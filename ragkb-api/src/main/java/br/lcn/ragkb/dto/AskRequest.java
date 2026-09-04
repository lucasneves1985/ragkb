package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question,
        String conversationId
) {}