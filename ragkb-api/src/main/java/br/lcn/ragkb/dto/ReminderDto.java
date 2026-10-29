package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.Reminder;
import br.lcn.ragkb.entity.ReminderStatus;

public record ReminderDto(
        String id,
        String conversationId,
        String originalRequest,
        String summary,
        Instant remindAt,
        Instant sentAt,
        ReminderStatus status,
        Integer retryCount,
        String lastError,
        Instant createdAt) {

    public static ReminderDto from(Reminder r) {
        return new ReminderDto(r.getId(), r.getConversationId(), r.getOriginalRequest(),
                r.getSummary(), r.getRemindAt(), r.getSentAt(), r.getStatus(),
                r.getRetryCount(), r.getLastError(), r.getCreatedAt());
    }
}
