package br.lcn.ragkb.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardStatus;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.CardRepository;
import br.lcn.ragkb.whatsapp.WhatsAppNotConnectedException;
import br.lcn.ragkb.whatsapp.WhatsAppSendException;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lembretes de prazo dos cards via WhatsApp (fase 2 da feature cards).
 *
 * AGENDAMENTO DINÂMICO: este job NÃO usa @Scheduled — é registrado e
 * reagendado pelo CardReminderScheduler, que lê o horário configurável em
 * app_configuration (cards.reminder_time, editável por ADMIN). Mudança de
 * horário vale em até 30s (ciclo de reconciliação), sem restart.
 *
 * JANELA CONFIGURÁVEL: cards.reminder_window_days (dias, editável por ADMIN)
 * define o tamanho da janela de alerta. Lida A CADA EXECUÇÃO — mudar a
 * janela vale na próxima execução diária, sem restart e sem tocar no
 * scheduler (a janela afeta a query de seleção, não o gatilho). Valor
 * ausente, não numérico ou fora dos limites (1-14): default 2 com warning —
 * a configuração errada nunca silencia os lembretes nem derruba o job.
 *
 * Regras de envio:
 * - Janela de alerta: prazo <= hoje + janela. O primeiro ciclo que encontrar
 * o card na janela envia UMA mensagem e marca due_reminder_sent_at — sem
 * reenvio para o mesmo prazo.
 * - Idempotência pela própria tabela (mesmo padrão do ReminderDispatchService):
 * nada em memória; restart da aplicação não duplica envio.
 * - Falha de envio mantém sent_at = NULL e o card é reprocessado na próxima
 * execução diária até conseguir, enquanto o prazo estiver na janela.
 * - Editar o prazo rearma o lembrete (Card.updateSchedule zera sent_at).
 * - Destinatário: telefone do cadastro do USUÁRIO QUE CRIOU O CARD
 * (Card.createdBy -> AppUser.phone), mesmo mecanismo do
 * ReminderDispatchService. Cards sem autoria (anteriores à V18) e criadores
 * sem telefone são pulados com warning — nunca marcados como enviados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardDueReminderJob {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int DEFAULT_WINDOW_DAYS = 2;
    private static final int MIN_WINDOW_DAYS = 1;
    private static final int MAX_WINDOW_DAYS = 14;

    private final CardRepository cardRepository;
    private final AppUserRepository userRepository;
    private final AppConfigurationService configurationService;
    private final WhatsAppService whatsAppService;

    public void sendDueReminders() {
        LocalDate today = LocalDate.now(ZONE);
        int windowDays = reminderWindowDays();
        LocalDate cutoff = today.plusDays(windowDays);
        List<Card> due = cardRepository
                .findByDueDateNotNullAndDueDateLessThanEqualAndStatusNotInAndDueReminderSentAtIsNullOrderByDueDateAsc(
                        cutoff, CardStatus.CLOSED_STATUSES);
        if (due.isEmpty()) {
            return;
        }

        log.info("Processando {} lembrete(s) de prazo de card (janela={} dias)", due.size(), windowDays);
        for (Card card : due) {
            dispatchToCreator(card, today);
        }
    }

    /**
     * Lê a janela configurada, com validação e fallback: ausente = default;
     * não numérico ou fora de [MIN, MAX] = default + warning. Lida a cada
     * execução — mudanças no banco valem na próxima varredura diária.
     */
    private int reminderWindowDays() {
        String value = configurationService
                .get(AppConfigurationService.KEY_CARDS_REMINDER_WINDOW_DAYS)
                .orElse(null);
        if (value == null || value.isBlank()) {
            return DEFAULT_WINDOW_DAYS;
        }
        try {
            int days = Integer.parseInt(value.trim());
            if (days < MIN_WINDOW_DAYS || days > MAX_WINDOW_DAYS) {
                log.warn("Valor fora dos limites para {} ({}): '{}' — usando default {} dias.",
                        AppConfigurationService.KEY_CARDS_REMINDER_WINDOW_DAYS,
                        MIN_WINDOW_DAYS + "-" + MAX_WINDOW_DAYS, value, DEFAULT_WINDOW_DAYS);
                return DEFAULT_WINDOW_DAYS;
            }
            return days;
        } catch (NumberFormatException e) {
            log.warn("Valor não numérico para {}: '{}' — usando default {} dias.",
                    AppConfigurationService.KEY_CARDS_REMINDER_WINDOW_DAYS, value, DEFAULT_WINDOW_DAYS);
            return DEFAULT_WINDOW_DAYS;
        }
    }

    private void dispatchToCreator(Card card, LocalDate today) {
        if (card.getCreatedBy() == null) {
            // Card anterior à V18: sem dono conhecido, não há para quem enviar.
            log.warn("Card {} sem created_by (anterior à V18) — lembrete de prazo ignorado.", card.getId());
            return;
        }
        AppUser creator = userRepository.findById(card.getCreatedBy()).orElse(null);
        if (creator == null) {
            log.warn("Criador {} do card {} não encontrado — lembrete ignorado.", card.getCreatedBy(), card.getId());
            return;
        }
        if (creator.getPhone() == null || creator.getPhone().isBlank()) {
            log.warn("Criador {} do card {} sem telefone no cadastro — lembrete ignorado.",
                    creator.getUsername(), card.getId());
            return;
        }

        String chatId = toChatId(creator.getPhone());
        try {
            whatsAppService.sendText(chatId, buildMessage(card, today));
            card.markDueReminderSent(Instant.now());
            cardRepository.save(card);
            log.info("Lembrete de prazo do card {} enviado para {} ({})",
                    card.getId(), creator.getUsername(), chatId);
        } catch (WhatsAppSendException | WhatsAppNotConnectedException e) {
            // sent_at permanece NULL — tentará novamente na próxima execução.
            log.warn("Envio do lembrete do card {} para {} falhou: {}",
                    card.getId(), creator.getUsername(), e.getMessage());
        }
    }

    private String buildMessage(Card card, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, card.getDueDate());
        String when;
        if (days < 0) {
            when = "ATRASADA há " + (-days) + (days == -1 ? " dia" : " dias");
        } else if (days == 0) {
            when = "vence HOJE (" + DATE_FORMAT.format(card.getDueDate()) + ")";
        } else {
            when = "vence em " + days + (days == 1 ? " dia" : " dias")
                    + " (" + DATE_FORMAT.format(card.getDueDate()) + ")";
        }
        return "⏰ Tarefa " + when + ": " + card.getTitle();
    }

    /**
     * Mesma conversão do ReminderDispatchService.toChatId (conservador: remove
     * não-dígitos; prefixa 55 se faltar DDI com 10-11 dígitos).
     */
    private String toChatId(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() >= 10 && digits.length() <= 11 && !digits.startsWith("55")) {
            digits = "55" + digits;
        }
        return digits + "@c.us";
    }
}