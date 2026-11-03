package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.CardStatusEvent;

public record CardStatusEventDto(
        String fromStatus,
        String toStatus,
        Instant changedAt) {

    public static CardStatusEventDto from(CardStatusEvent e) {
        return new CardStatusEventDto(
                e.getFromStatus() == null ? null : e.getFromStatus().name(),
                e.getToStatus().name(),
                e.getChangedAt());
    }
}
