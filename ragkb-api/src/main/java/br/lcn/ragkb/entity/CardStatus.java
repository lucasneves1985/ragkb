package br.lcn.ragkb.entity;

import java.util.List;

public enum CardStatus {
    PENDENTE, EM_ANDAMENTO, AGUARDANDO_FORNECEDOR, AGUARDANDO_RETORNO, CONCLUIDO, CANCELADO;

    /**
     * Encerradas = concluídas + canceladas (mesmo toggle no board).
     */
    public static final List<CardStatus> CLOSED_STATUSES = List.of(CONCLUIDO, CANCELADO);

    public boolean isClosed() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
