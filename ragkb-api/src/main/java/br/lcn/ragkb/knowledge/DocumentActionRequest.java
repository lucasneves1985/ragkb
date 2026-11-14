package br.lcn.ragkb.knowledge;



import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record DocumentActionRequest(
        @NotBlank String action,          // ARCHIVE | REACTIVATE
        List<String> allowedRoles         // opcional: usado na reativação
) {}