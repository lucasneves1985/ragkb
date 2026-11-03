package br.lcn.ragkb.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.service.CardDigestJob;
import br.lcn.ragkb.service.CardDueReminderJob;
import lombok.RequiredArgsConstructor;

/**
 * Testes "Checar Agora" das notificações de cards (lembrete de prazo e
 * digest). ADMIN-only. Cada teste envia UMA mensagem de WhatsApp APENAS para
 * o telefone do admin solicitante (resolvido pelo JWT) — nunca para os demais
 * usuários — e não altera nenhum estado (não marca due_reminder_sent_at).
 */
@RestController
@RequestMapping("/api/cards/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CardNotificationsTestController {

    private final CardDueReminderJob reminderJob;
    private final CardDigestJob digestJob;

    public record NotificationTestResponse(boolean sent, String detail, String preview, int cardsFound) {
    }

    @PostMapping("/test-reminder")
    public NotificationTestResponse testReminder(Authentication auth) {
        CardDueReminderJob.ReminderTestResult result = reminderJob.testReminderFor(auth.getName());
        return new NotificationTestResponse(result.sent(), result.detail(), result.preview(), result.cardsInWindow());
    }

    @PostMapping("/test-digest")
    public NotificationTestResponse testDigest(Authentication auth) {
        CardDigestJob.DigestTestResult result = digestJob.testDigestFor(auth.getName());
        return new NotificationTestResponse(result.sent(), result.detail(), result.preview(), result.cardsFound());
    }
}