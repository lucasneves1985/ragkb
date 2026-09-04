package br.lcn.ragkb.eval;

public record EvalItem(
        String id,
        String question,
        String expectedClass,   // HAS_ANSWER | NO_ANSWER | NEAR_MISS
        String expectedDocId,    // obrigatório para HAS_ANSWER
        String expectedAnswer,
        String sector
) {}