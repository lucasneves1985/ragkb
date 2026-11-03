package br.lcn.ragkb.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * FORMATO AGREGADO (decisão do autor): cada criador recebe UMA mensagem com
 * TODAS as suas tarefas na janela, ordenadas da MAIS urgente para a menos:
 * atrasada mais antiga → atrasada mais recente → vence hoje → vence em N
 * dias (crescente). A ordem sai da query (due_date ASC) preservada no
 * agrupamento (LinkedHashMap). Uma mensagem por criador substitui o modelo
 * anterior de uma mensagem por card.
 *
 * FORMATAÇÃO WHATSAPP: o cabeçalho usa negrito nativo do WhatsApp
 * (*texto*); os emojis ficam apenas nos itens (estado de cada tarefa) —
 * o cabeçalho vai limpo, sem ícone.
 *
 * Regras de envio:
 * - Janela de alerta: prazo <= hoje + janela. O primeiro ciclo que encontrar
 * os cards na janela envia a mensagem agregada e marca due_reminder_sent_at
 * de TODOS os cards incluídos — sem reenvio para o mesmo prazo.
 * - Marcação em lote ATÔMICA na prática: se o envio falha, NENHUM card do
 * criador é marcado — a próxima execução reenvia a mensagem completa.
 * - Idempotência pela própria tabela (mesmo padrão do ReminderDispatchService):
 * nada em memória; restart da aplicação não duplica envio.
 * - Editar o prazo rearma o lembrete (Card.updateSchedule zera sent_at).
 * - Destinatário: telefone do cadastro do USUÁRIO QUE CRIOU O CARD
 * (Card.createdBy -> AppUser.phone), mesmo mecanismo do
 * ReminderDispatchService. Cards sem autoria (anteriores à V18) e criadores
 * sem telefone são pulados com warning — nunca marcados como enviados.
 *
 * TESTE ("Checar Agora"): testReminderFor envia APENAS para o admin que
 * solicitou, com a mensagem completa dos cards DELE na janela (inclusive os
 * já notificados — preview fiel do que a janela contém). NUNCA marca
 * due_reminder_sent_at — o teste não consome a idempotência dos lembretes
 * reais.
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
    private static final int MAX_TITLE_LENGTH = 60;

    private final CardRepository cardRepository;
    private final AppUserRepository userRepository;
    private final AppConfigurationService configurationService;
    private final WhatsAppService whatsAppService;

    /**
     * Resultado do teste "Checar Agora": sent indica se a mensagem saiu,
     * detail descreve o resultado para a UI, preview é o texto exato que o
     * job produziria e cardsInWindow é o total de cards de TODOS os usuários
     * na janela (o que o job real processaria).
     */
    public record ReminderTestResult(boolean sent, String detail, String preview, int cardsInWindow) {
    }

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

        // Agrupa por criador preservando a ordem de urgência (due_date ASC).
        // LinkedHashMap garante que a lista de cada criador chega ordenada.
        Map<Long, List<Card>> byCreator = new LinkedHashMap<>();
        for (Card card : due) {
            if (card.getCreatedBy() == null) {
                // Card anterior à V18: sem dono conhecido, não há para quem enviar.
                log.warn("Card {} sem created_by (anterior à V18) — lembrete de prazo ignorado.", card.getId());
                continue;
            }
            byCreator.computeIfAbsent(card.getCreatedBy(), k -> new ArrayList<>()).add(card);
        }

        log.info("Processando lembretes de prazo: {} card(s) na janela para {} criador(es) (janela={} dias)",
                due.size(), byCreator.size(), windowDays);
        for (Map.Entry<Long, List<Card>> entry : byCreator.entrySet()) {
            dispatchToCreator(entry.getKey(), entry.getValue(), today, windowDays);
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

    private void dispatchToCreator(Long creatorId, List<Card> cards, LocalDate today, int windowDays) {
        AppUser creator = userRepository.findById(creatorId).orElse(null);
        if (creator == null) {
            log.warn("Criador {} não encontrado — lembrete de {} card(s) ignorado.", creatorId, cards.size());
            return;
        }
        if (creator.getPhone() == null || creator.getPhone().isBlank()) {
            log.warn("Criador {} sem telefone no cadastro — lembrete de {} card(s) ignorado.",
                    creator.getUsername(), cards.size());
            return;
        }

        String chatId = toChatId(creator.getPhone());
        try {
            whatsAppService.sendText(chatId, buildMessage(cards, today, windowDays));
            // Marcação em lote: só executa se o envio teve sucesso — falha
            // deixa TODOS os cards da mensagem para a próxima execução.
            for (Card card : cards) {
                card.markDueReminderSent(Instant.now());
                cardRepository.save(card);
            }
            log.info("Lembrete de prazo agregado enviado para {} ({}): {} card(s)",
                    creator.getUsername(), chatId, cards.size());
        } catch (WhatsAppSendException | WhatsAppNotConnectedException e) {
            // Nenhum card marcado — tentará novamente na próxima execução.
            log.warn("Envio do lembrete para {} ({} card(s)) falhou: {}",
                    creator.getUsername(), cards.size(), e.getMessage());
        }
    }

    /**
     * Mensagem agregada: cabeçalho em NEGRITO nativo do WhatsApp (*texto*),
     * sem ícone; cada tarefa em uma linha, da mais urgente para a menos (a
     * lista chega ordenada por due_date ASC da query).
     */
    private String buildMessage(List<Card> cards, LocalDate today, int windowDays) {
        StringBuilder sb = new StringBuilder("*Lembrete de tarefas* — ")
                .append(cards.size()).append(cards.size() == 1 ? " tarefa" : " tarefas")
                .append(" na janela de ").append(windowDays).append(" dia(s):\n");
        for (Card card : cards) {
            sb.append("• ").append(describeWhen(card.getDueDate(), today))
                    .append(": ").append(truncate(card.getTitle())).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String describeWhen(LocalDate dueDate, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, dueDate);
        if (days < 0) {
            return "🔴 ATRASADA há " + (-days) + (days == -1 ? " dia" : " dias")
                    + " (vencia em " + DATE_FORMAT.format(dueDate) + ")";
        }
        if (days == 0) {
            return "⏰ vence HOJE (" + DATE_FORMAT.format(dueDate) + ")";
        }
        return "📅 vence em " + days + (days == 1 ? " dia" : " dias")
                + " (" + DATE_FORMAT.format(dueDate) + ")";
    }

    private String truncate(String title) {
        return title.length() <= MAX_TITLE_LENGTH ? title : title.substring(0, MAX_TITLE_LENGTH - 1) + "…";
    }

    /**
     * Teste "Checar Agora" dos lembretes: envia APENAS para o admin
     * solicitante, com a mensagem completa dos cards DELE na janela. Usa a
     * query SEM filtro de sent_at — o preview mostra TODOS os cards na
     * janela, inclusive os já notificados (fiel ao conteúdo da janela, não
     * ao que ainda falta notificar). NUNCA marca sent_at.
     */
    public ReminderTestResult testReminderFor(String username) {
        LocalDate today = LocalDate.now(ZONE);
        int windowDays = reminderWindowDays();
        LocalDate cutoff = today.plusDays(windowDays);
        List<Card> inWindow = cardRepository
                .findByDueDateNotNullAndDueDateLessThanEqualAndStatusNotInOrderByDueDateAsc(
                        cutoff, CardStatus.CLOSED_STATUSES);

        AppUser admin = userRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            return new ReminderTestResult(false,
                    "Usuário autenticado não encontrado no cadastro.", null, inWindow.size());
        }
        if (admin.getPhone() == null || admin.getPhone().isBlank()) {
            return new ReminderTestResult(false,
                    "Seu cadastro não tem telefone — o teste não pode ser entregue. Preencha o telefone no seu perfil.",
                    null, inWindow.size());
        }

        List<Card> own = inWindow.stream()
                .filter(c -> admin.getId().equals(c.getCreatedBy()))
                .toList();

        String preview = own.isEmpty()
                ? "Nenhum card seu está na janela de lembretes (" + windowDays + " dia(s)). "
                        + "Mensagem de teste do canal — nenhum lembrete real seria enviado para você agora."
                : buildMessage(own, today, windowDays);

        try {
            whatsAppService.sendText(toChatId(admin.getPhone()), preview);
            String detail = "Mensagem enviada para o seu WhatsApp. " + inWindow.size()
                    + " card(s) no total na janela de " + windowDays + " dia(s); o preview abaixo lista TODOS os "
                    + "SEUS cards na janela, do mais urgente ao menos urgente. O teste não marca nada como enviado.";
            log.info("Teste de lembrete de prazo enviado para {} ({})", admin.getUsername(), admin.getPhone());
            return new ReminderTestResult(true, detail, preview, inWindow.size());
        } catch (WhatsAppSendException | WhatsAppNotConnectedException e) {
            log.warn("Teste de lembrete para {} falhou: {}", admin.getUsername(), e.getMessage());
            return new ReminderTestResult(false, "Falha no envio pelo WhatsApp: " + e.getMessage(), preview,
                    inWindow.size());
        }
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