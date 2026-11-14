package br.lcn.ragkb.reminder;



import java.time.Instant;

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
