package br.lcn.ragkb.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * Digest de tarefas por WhatsApp — panorama diário/semanal por usuário.
 *
 * AGENDAMENTO DINÂMICO: registrado pelo CardDigestScheduler a partir das
 * configurações globais (cards.digest_enabled, cards.digest_frequency,
 * cards.digest_time, cards.digest_weekday — editáveis por ADMIN).
 *
 * ESCOPO (V20): cada destinatário recebe cards PRÓPRIOS + cards de projetos
 * COMPARTILHADOS onde participa (owner ou share) — o supervisor monitora o
 * projeto inteiro sem criar cards; o analista vê cards de colegas. Projetos
 * legados NÃO ampliam o escopo (privacidade por criador preservada).
 *
 * Diferenças em relação ao lembrete de prazo (CardDueReminderJob):
 * - É RETRATO SITUACIONAL, não evento por card: NÃO marca estado (nada de
 *   sent_at) — a idempotência vem do próprio cron (uma execução por ciclo).
 * - Cards já notificados pelo lembrete continuam no digest: aviso pontual e
 *   panorama são propósitos distintos.
 * - SILÊNCIO por design: usuário sem card acessível atrasado/vencendo não
 *   recebe mensagem — digest vazio é ruído que treina o usuário a ignorar o
 *   canal.
 * - Destinatário: telefone do cadastro do PRÓPRIO destinatário; usuários sem
 *   telefone são pulados com warning. O lembrete de prazo continua roteado ao
 *   CRIADOR do card — os dois fluxos são independentes.
 *
 * TESTE ("Checar Agora"): testDigestFor envia APENAS para o admin que
 * solicitou, com o digest DELE (mesmo escopo do digest real). Diferente do
 * digest real, o teste envia MESMO com buckets vazios (com aviso explicativo)
 * — o objetivo do teste é validar o canal e a formatação, não simular o
 * silêncio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardDigestJob {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int HORIZON_DAYS = 7;
    private static final int MAX_TITLE_LENGTH = 60;

    private final CardRepository cardRepository;
    private final AppUserRepository userRepository;
    private final WhatsAppService whatsAppService;

    /**
     * Resultado do teste "Checar Agora": sent indica se a mensagem saiu,
     * detail descreve o resultado para a UI, preview é o texto exato do
     * resumo e cardsFound é o total de cards do admin nos buckets.
     */
    public record DigestTestResult(boolean sent, String detail, String preview, int cardsFound) {
    }

    public void sendDigest() {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate horizon = today.plusDays(HORIZON_DAYS);

        // Destinatários: criadores com cards abertos no horizonte + owners e
        // participantes de projetos compartilhados com cards abertos no
        // horizonte (união deduplicada — um usuário pode estar nas três listas).
        Set<Long> recipientIds = new LinkedHashSet<>(
                cardRepository.findDistinctCreatorIdsOfOpenCards(CardStatus.CLOSED_STATUSES));
        recipientIds.addAll(cardRepository.findOwnerIdsWithDueCards(CardStatus.CLOSED_STATUSES, horizon));
        recipientIds.addAll(cardRepository.findShareParticipantIdsWithDueCards(CardStatus.CLOSED_STATUSES, horizon));
        if (recipientIds.isEmpty()) {
            return;
        }

        int sent = 0;
        for (Long userId : recipientIds) {
            if (dispatchToUser(userId, today, horizon)) {
                sent++;
            }
        }
        log.info("Digest de tarefas: {} resumo(s) enviado(s) para {} destinatário(s) com cards acessíveis no horizonte",
                sent, recipientIds.size());
    }

    private boolean dispatchToUser(Long userId, LocalDate today, LocalDate horizon) {
        List<Card> due = cardRepository.findDueCardsAccessibleTo(userId, CardStatus.CLOSED_STATUSES, horizon);
        List<Card> overdue = new ArrayList<>();
        List<Card> dueToday = new ArrayList<>();
        List<Card> upcoming = new ArrayList<>();
        for (Card card : due) {
            if (card.getDueDate().isBefore(today)) {
                overdue.add(card);
            } else if (card.getDueDate().isEqual(today)) {
                dueToday.add(card);
            } else {
                upcoming.add(card);
            }
        }
        if (overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty()) {
            return false; // nada relevante — silêncio por design
        }
        AppUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Digest: destinatário {} não encontrado — resumo ignorado.", userId);
            return false;
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            log.warn("Digest: destinatário {} sem telefone no cadastro — resumo ignorado.", user.getUsername());
            return false;
        }
        String chatId = toChatId(user.getPhone());
        try {
            whatsAppService.sendText(chatId, buildMessage(today, overdue, dueToday, upcoming));
            log.info("Digest de tarefas enviado para {} ({})", user.getUsername(), chatId);
            return true;
        } catch (WhatsAppSendException | WhatsAppNotConnectedException e) {
            log.warn("Envio do digest para {} falhou: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Teste "Checar Agora" do digest: envia APENAS para o admin solicitante,
     * com o digest DELE (mesmo escopo do digest real: próprios + compartilhados).
     * Envia mesmo com buckets vazios (com aviso) — o teste valida o canal e a
     * formatação, não o silêncio do digest real.
     */
    public DigestTestResult testDigestFor(String username) {
        LocalDate today = LocalDate.now(ZONE);
        LocalDate horizon = today.plusDays(HORIZON_DAYS);
        AppUser admin = userRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            return new DigestTestResult(false, "Usuário autenticado não encontrado no cadastro.", null, 0);
        }
        if (admin.getPhone() == null || admin.getPhone().isBlank()) {
            return new DigestTestResult(false,
                    "Seu cadastro não tem telefone — o teste não pode ser entregue. Preencha o telefone no seu perfil.",
                    null, 0);
        }
        List<Card> due = cardRepository.findDueCardsAccessibleTo(admin.getId(), CardStatus.CLOSED_STATUSES, horizon);
        List<Card> overdue = new ArrayList<>();
        List<Card> dueToday = new ArrayList<>();
        List<Card> upcoming = new ArrayList<>();
        for (Card card : due) {
            if (card.getDueDate().isBefore(today)) {
                overdue.add(card);
            } else if (card.getDueDate().isEqual(today)) {
                dueToday.add(card);
            } else {
                upcoming.add(card);
            }
        }
        int total = overdue.size() + dueToday.size() + upcoming.size();
        String preview = total == 0
                ? "Seu digest estaria VAZIO agora (silêncio por design — nenhuma mensagem seria enviada). "
                        + "Esta é uma mensagem de teste do canal."
                : buildMessage(today, overdue, dueToday, upcoming);
        try {
            whatsAppService.sendText(toChatId(admin.getPhone()), preview);
            String detail = "Resumo de teste enviado para o seu WhatsApp. " + total
                    + " card(s) nos buckets (atrasadas / vencem hoje / próximos 7 dias).";
            log.info("Teste de digest enviado para {} ({})", admin.getUsername(), admin.getPhone());
            return new DigestTestResult(true, detail, preview, total);
        } catch (WhatsAppSendException | WhatsAppNotConnectedException e) {
            log.warn("Teste de digest para {} falhou: {}", admin.getUsername(), e.getMessage());
            return new DigestTestResult(false, "Falha no envio pelo WhatsApp: " + e.getMessage(), preview, total);
        }
    }

    private String buildMessage(LocalDate today, List<Card> overdue, List<Card> dueToday, List<Card> upcoming) {
        StringBuilder sb = new StringBuilder("📋 Resumo das suas tarefas — ")
                .append(DATE_FORMAT.format(today)).append('\n');
        appendBucket(sb, "🔴 Atrasadas", "vencia em", overdue);
        appendBucket(sb, "⏰ Vencem hoje", null, dueToday);
        appendBucket(sb, "📅 Próximos 7 dias", "vence em", upcoming);
        return sb.toString().stripTrailing();
    }

    private void appendBucket(StringBuilder sb, String header, String verb, List<Card> cards) {
        if (cards.isEmpty()) {
            return;
        }
        sb.append('\n').append(header).append(" (").append(cards.size()).append("):\n");
        for (Card card : cards) {
            sb.append("• ").append(truncate(card.getTitle()));
            if (verb != null) {
                sb.append(" — ").append(verb).append(' ').append(DATE_FORMAT.format(card.getDueDate()));
            }
            sb.append('\n');
        }
    }

    private String truncate(String title) {
        return title.length() <= MAX_TITLE_LENGTH ? title : title.substring(0, MAX_TITLE_LENGTH - 1) + "…";
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