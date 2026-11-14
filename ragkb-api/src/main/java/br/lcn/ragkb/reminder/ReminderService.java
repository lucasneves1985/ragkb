package br.lcn.ragkb.reminder;







import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.reminder.api.ReminderAssistantApi;
import br.lcn.ragkb.reminder.api.ReminderDto;
import br.lcn.ragkb.identity.api.UserLookupApi;
import br.lcn.ragkb.shared.exception.ReminderNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderService implements ReminderAssistantApi {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final ReminderRepository repository;
    private final UserLookupApi userLookupApi;
    private final ReminderExtractionService extractionService;

    /**
     * Cria um lembrete a partir de texto livre, extraindo data/hora via LLM. O
     * envio usa o telefone do cadastro do usuário.
     */
    @Transactional
    @Override
    public ReminderDto create(String userText, String username, String conversationId) {
        var user = userLookupApi.findContactByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));

        if (user.phone() == null || user.phone().isBlank()) {
            throw new IllegalArgumentException(
                    "Seu cadastro não tem telefone. Atualize o perfil para receber lembretes via WhatsApp.");
        }

        var extraction = extractionService.extract(userText)
                .orElseThrow(() -> new IllegalArgumentException(
                "Não conseguir identificar a data e hora do lembrete. Informe explicitamente, "
                + "ex.: 'me lembre de X amanhã às 15:00'."));

        Instant remindAt = extraction.localDateTime().atZone(ZONE).toInstant();
        if (!remindAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "A data/hora do lembrete (" + extraction.localDateTime() + ") está no passado.");
        }

        var reminder = new Reminder(user.id(), conversationId, userText,
                extraction.summary(), remindAt);
        return toDto(repository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderDto> listActive(String username) {
        var user = userLookupApi.findContactByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
        return repository.findByUserIdAndStatusOrderByRemindAtDesc(user.id(), ReminderStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void cancel(String id, String username) {
        var user = userLookupApi.findContactByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
        Reminder reminder = repository.findByIdAndUserId(id, user.id())
                .orElseThrow(() -> new ReminderNotFoundException(id));
        reminder.cancel();
        repository.save(reminder);
    }

    private ReminderDto toDto(Reminder reminder) {
        return new ReminderDto(reminder.getId(), reminder.getConversationId(), reminder.getOriginalRequest(),
                reminder.getSummary(), reminder.getRemindAt(), reminder.getSentAt(), reminder.getStatus().name(),
                reminder.getRetryCount(), reminder.getLastError(), reminder.getCreatedAt());
    }
}
