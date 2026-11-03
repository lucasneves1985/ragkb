package br.lcn.ragkb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pergunta em linguagem natural sobre tarefas. projectId é filtro opcional —
 * null busca em todos os projetos (escopo pessoal, sem risco de vazamento).
 */
public record CardAskRequest(
        @NotBlank @Size(max = 2000) String question,
        String projectId) {
}