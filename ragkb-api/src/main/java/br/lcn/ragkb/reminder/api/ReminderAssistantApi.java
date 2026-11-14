package br.lcn.ragkb.reminder.api;

import br.lcn.ragkb.reminder.api.ReminderDto;

public interface ReminderAssistantApi {
    ReminderDto create(String userText, String username, String conversationId);
}
