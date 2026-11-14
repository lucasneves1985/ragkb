package br.lcn.ragkb.assistant;



import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question,
        String conversationId
) {}