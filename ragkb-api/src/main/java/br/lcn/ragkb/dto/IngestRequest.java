package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IngestRequest(
        @NotBlank String sector,
        @Size(min = 1) List<String> allowedRoles,
        @Size(min = 1) List<String> allowedSectors,
        String supersedesDocumentId // opcional
) {}