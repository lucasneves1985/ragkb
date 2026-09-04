package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String category,
        @NotBlank String subject,
        @NotBlank String description,
        @NotBlank String requester
) {}
