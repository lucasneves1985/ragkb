package br.lcn.ragkb.dto;

/**
 * Referência clicável de card na resposta do assistente — a UI navega
 * para o board do projeto com o detalhe do card aberto.
 */
public record CardReferenceDto(
        String cardId,
        String cardTitle,
        String projectId,
        String projectName,
        String status) {
}