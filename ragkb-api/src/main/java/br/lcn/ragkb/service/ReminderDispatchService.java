package br.lcn.ragkb.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Reminder;
import br.lcn.ragkb.entity.ReminderStatus;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.ReminderRepository;
import br.lcn.ragkb.whatsapp.WhatsAppSendException;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispatcher de lembretes por polling: a cada 30s varre PENDING vencidos, gera
 * o texto da mensagem via LLM (no disparo, não antecipado) e envia via WAHA
 * para o telefone do cadastro do usuário.
 *
 * Idempotência e sobrevivência a restart: a fila é a própria tabela — nada em
 * memória. Falha de envio mantém PENDING com retry_count++; após 3 tentativas
 * vira FAILED visível na listagem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatchService.class);
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter BRAZIL_FORMAT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(ZONE);

    private final ReminderRepository reminderRepository;
    private final AppUserRepository userRepository;
    private final WhatsAppService whatsAppService;
    private final ChatClient chatClient;

    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void dispatchDue() {
        List<Reminder> due = reminderRepository
                .findTop20ByStatusAndRemindAtLessThanEqualOrderByRemindAtAsc(
                        ReminderStatus.PENDING, Instant.now());
        for (Reminder reminder : due) {
            try {
                dispatch(reminder);
            } catch (Exception e) {
                log.error("Erro inesperado ao processar lembrete {}: {}", reminder.getId(), e.getMessage(), e);
            }
        }
    }

    private void dispatch(Reminder reminder) {
        AppUser user = userRepository.findById(reminder.getUserId()).orElse(null);
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            reminder.registerRetryFailure("Usuário não encontrado ou sem telefone no cadastro.");
            reminderRepository.save(reminder);
            return;
        }
        String chatId = toChatId(user.getPhone());
        String message = generateMessage(reminder, user);
        try {
            whatsAppService.sendText(chatId, message);
            reminder.markSent();
            reminderRepository.save(reminder);
            log.info("Lembrete {} enviado para {}", reminder.getId(), chatId);
        } catch (WhatsAppSendException e) {
            reminder.registerRetryFailure(e.getMessage());
            reminderRepository.save(reminder);
            log.warn("Envio do lembrete {} falhou (tentativa {}): {}",
                    reminder.getId(), reminder.getRetryCount(), e.getMessage());
        }
    }

    /**
     * Segunda chamada LLM: texto redigido no momento do disparo.
     */
    private String generateMessage(Reminder reminder, AppUser user) {
        String prompt = """
                Escreva a mensagem de um lembrete enviado por WhatsApp.
                Conteúdo do lembrete: %s
                Hora agendada para lembrar: %s
                Nome do usuário: %s
                A mensagem deve ter no máximo 2 frases, tom cordial e direto,
                dirigida ao usuário em primeira pessoa do sistema ("Lembrete: ...").
                Responda APENAS com o texto da mensagem.
                """.formatted(
                reminder.getOriginalRequest() == null ? "" : reminder.getOriginalRequest(),
                reminder.getRemindAt() == null ? "" : BRAZIL_FORMAT.format(reminder.getRemindAt()),
                user.getFullName());
        try {
            String content = chatClient.prompt().user(prompt).call().content();
            return content == null || content.isBlank() ? defaultText(reminder) : content.trim();
        } catch (Exception e) {
            // LLM indisponível no momento crítico — degrade para mensagem fixa
            log.warn("Geração de texto via LLM falhou; usando texto padrão: {}", e.getMessage());
            return defaultText(reminder);
        }
    }

    private String defaultText(Reminder reminder) {
        return "Lembrete: %s (%s)".formatted(
                reminder.getSummary() == null ? reminder.getOriginalRequest() : reminder.getSummary(),
                reminder.getRemindAt() == null ? "" : BRAZIL_FORMAT.format(reminder.getRemindAt()));
    }

    /**
     * Converte o telefone do cadastro em chatId da WAHA. Conservador: remove
     * não-dígitos; prefixa 55 se faltar DDI (10-11 dígitos). Se o cadastro
     * gravar telefone sem DDI completo, este conversor corrige; telefone
     * inválido no cadastro gera chatId errado — criticalidade média.
     */
    String toChatId(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 10 && digits.length() <= 11 && !digits.startsWith("55")) {
            digits = "55" + digits;
        }
        return digits + "@c.us";
    }
}
