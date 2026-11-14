package br.lcn.ragkb.reminder.api;

import java.time.Instant;

public record ReminderDto(
        String id,
        String conversationId,
        String originalRequest,
        String summary,
        Instant remindAt,
        Instant sentAt,
        String status,
        Integer retryCount,
        String lastError,
        Instant createdAt) {

}
