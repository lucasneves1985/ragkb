package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCardRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        String description) {

}
