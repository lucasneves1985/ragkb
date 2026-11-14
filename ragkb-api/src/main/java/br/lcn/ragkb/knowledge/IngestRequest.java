package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestRequest(
        @NotBlank String sector,
        @Size(min = 1) List<String> allowedRoles,
        @Size(min = 1) List<String> allowedSectors,
        String supersedesDocumentId // opcional
) {}