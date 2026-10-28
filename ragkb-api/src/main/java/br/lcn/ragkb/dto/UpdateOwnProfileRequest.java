package br.lcn.ragkb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOwnProfileRequest(
        @NotBlank @Size(min = 3, max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email
) {}
