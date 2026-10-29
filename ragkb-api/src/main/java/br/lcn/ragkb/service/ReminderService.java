package br.lcn.ragkb.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.ReminderDto;
import br.lcn.ragkb.entity.Reminder;
import br.lcn.ragkb.entity.ReminderStatus;
import br.lcn.ragkb.exception.ReminderNotFoundException;
import br.lcn.ragkb.repository.ReminderRepository;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.entity.AppUser;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final ReminderRepository repository;
    private final AppUserRepository userRepository;
    private final ReminderExtractionService extractionService;

    /**
     * Cria um lembrete a partir de texto livre, extraindo data/hora via LLM. O
     * envio usa o telefone do cadastro do usuário.
     */
    @Transactional
    public ReminderDto create(String userText, String username, String conversationId) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));

        if (user.getPhone() == null || user.getPhone().isBlank()) {
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

        var reminder = new Reminder(user.getId(), conversationId, userText,
                extraction.summary(), remindAt);
        return ReminderDto.from(repository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderDto> listActive(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
        return repository.findByUserIdAndStatusOrderByRemindAtDesc(user.getId(), ReminderStatus.PENDING)
                .stream().map(ReminderDto::from).toList();
    }

    @Transactional
    public void cancel(String id, String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + username));
        Reminder reminder = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ReminderNotFoundException(id));
        reminder.cancel();
        repository.save(reminder);
    }
}
