package br.lcn.ragkb.identity;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectorRequest(
        @NotBlank @Size(max = 80) String name
) {}