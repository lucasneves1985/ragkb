package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * position: índice de destino dentro da coluna (0-based). Null = fim da coluna.
 * A UI de drag-and-drop manda o índice onde o card foi solto; índices fora do
 * range são clampados no service (robustez contra corrida entre a renderização
 * do board e o save).
 */
public record MoveCardRequest(
        @NotBlank
        String status,
        Integer position) {

}
