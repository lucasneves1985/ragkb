package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.CardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * status: opcional — criação estilo Trello direto na coluna. Null = PENDENTE. O
 * evento de criação é gravado como null -> status informado, nunca uma
 * transição falsa PENDENTE -> alvo.
 */
public record CreateCardRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        String description,
        CardStatus status) {

}
