package br.lcn.ragkb.knowledge;



import jakarta.validation.constraints.NotBlank;

public record ConflictActionRequest(
        @NotBlank String action,  // REVIEWED | RESOLVED | DISMISSED
        String note               // opcional: justificativa (auditoria)
) {}