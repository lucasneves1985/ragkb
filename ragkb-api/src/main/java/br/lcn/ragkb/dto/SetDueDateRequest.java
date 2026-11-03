package br.lcn.ragkb.dto;

import java.time.LocalDate;

/**
 * dueDate null = remove o prazo. Endpoint dedicado (PATCH
 * /api/cards/{id}/due-date)
 * justamente para NÃO misturar essa semântica de null com o UpdateCardRequest,
 * onde campo ausente e null são indistinguíveis.
 */
public record SetDueDateRequest(LocalDate dueDate) {
}