package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.Sector;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull Sector sector,
        @Size(min = 1) List<String> roles
) {}