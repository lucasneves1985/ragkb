package br.lcn.ragkb.identity;



import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 3, max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 40) String phone,
        @NotNull Long sectorId,
        @Size(min = 1) List<String> roles
) {}