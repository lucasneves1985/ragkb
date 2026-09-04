package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DocumentActionRequest(
        @NotBlank String action,          // ARCHIVE | REACTIVATE
        List<String> allowedRoles         // opcional: usado na reativação
) {}