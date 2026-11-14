package br.lcn.ragkb.assistant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import br.lcn.ragkb.reminder.api.ReminderAssistantApi;
import br.lcn.ragkb.reminder.api.ReminderDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderAnswerHandler {

    /**
     * Detecção determinística de intenção de lembrete (frente 4).
     */
    private static final Pattern REMINDER_INTENT = Pattern.compile(
            "^\\s*(me\\s+lembre|me\\s+lembra|lembre-me|lembra-?me"
            + "|quero\\s+um\\s+lembrete|crie\\s+um\\s+lembrete|criar\\s+um\\s+lembrete"
            + "|lembrar\\s+de|lembrete\\s*:)\\b.*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZONE);

    private final ReminderAssistantApi reminderService;
    private final AssistantConversationRecorder conversationRecorder;

    public boolean matches(String question) {
        return REMINDER_INTENT.matcher(question.trim()).matches();
    }

    /** Intent de lembrete: mesma lógica da frente 4 (inalterada). */
    public AnswerResponse handle(String question, AssistantContext context) {
        ReminderDto reminder;
        try {
            reminder = reminderService.create(question, context.userId(), context.conversationId());
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() != null ? e.getMessage()
                    : "Não consegui criar o lembrete. Informe a data e a hora explicitamente.";
            AnswerResponse response = new AnswerResponse("REMINDER_REJECTED", message,
                    List.of(), List.of(), null, context.conversationId());
            conversationRecorder.record(context.conversationId(), question, response);
            return response;
        }
        String summary = reminder.summary() == null || reminder.summary().isBlank()
                ? question
                : reminder.summary();
        String message = "Lembrete agendado: %s. Vou te lembrar no WhatsApp em %s.".formatted(
                summary.trim(), REMINDER_TIME_FORMAT.format(reminder.remindAt()));
        AnswerResponse response = AnswerResponse.fromReminder(message, context.conversationId());
        conversationRecorder.record(context.conversationId(), question, response);
        return response;
    }
}
