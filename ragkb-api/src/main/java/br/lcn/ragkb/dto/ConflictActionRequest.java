package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

public record ConflictActionRequest(
        @NotBlank String action,  // REVIEWED | RESOLVED | DISMISSED
        String note               // opcional: justificativa (auditoria)
) {}