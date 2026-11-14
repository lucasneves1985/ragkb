package br.lcn.ragkb.card;



import java.time.Instant;

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
