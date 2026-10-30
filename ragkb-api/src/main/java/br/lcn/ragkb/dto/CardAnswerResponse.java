package br.lcn.ragkb.dto;

import java.util.List;

/**
 * status: ANSWERED (resposta composta com candidatos) | NO_MATCH (nenhuma
 * tarefa encontrada — resposta fixa, sem LLM).
 */
public record CardAnswerResponse(
        String status,
        String answer,
        List<CardReferenceDto> cards) {

    public static CardAnswerResponse answered(String answer, List<CardReferenceDto> cards) {
        return new CardAnswerResponse("ANSWERED", answer, cards);
    }

    public static CardAnswerResponse noMatch() {
        return new CardAnswerResponse("NO_MATCH",
                "Não encontrei nenhuma tarefa relacionada a essa pergunta.", List.of());
    }
}