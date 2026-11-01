package br.lcn.ragkb.service;

import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Agendamento dinâmico do CardDueReminderJob a partir da configuração global
 * (app_configuration -> cards.reminder_time, HH:mm, editável apenas por ADMIN).
 *
 * Modelo de reconciliação — MESMO padrão do IntegrationScheduler: a cada 30s
 * compara o valor configurado com o job registrado e cancela/registra o que
 * mudou. Vantagens: não depende de hooks no controller de configuração,
 * sobrevive a restart (reconcilia no boot) e nunca fica com job órfão.
 * Trade-off: mudança de horário leva até 30s para valer.
 *
 * Comportamento do cron diário: se o horário configurado já passou hoje, a
 * próxima execução é AMANHÃ no horário configurado (CronTrigger) — lembretes
 * que entrarem na janela depois da hora do dia saem no ciclo seguinte.
 *
 * Valor ausente ou inválido no banco: usa o default 08:00 e loga warning —
 * nunca deixa o lembrete sem agendamento nem derruba a aplicação.
 *
 * Limitação aceita: single-instância (mesma do IntegrationScheduler).
 */
@Service
@RequiredArgsConstructor
public class CardReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(CardReminderScheduler.class);
    private static final long RECONCILE_INTERVAL_MS = 30_000;
    private static final String DEFAULT_TIME = "08:00";
    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final TaskScheduler integrationTaskScheduler;
    private final AppConfigurationService configurationService;
    private final CardDueReminderJob reminderJob;

    private ScheduledFuture<?> job;
    private String fingerprint;

    @PostConstruct
    void reconcileOnStartup() {
        reconcile();
    }

    @Scheduled(fixedDelay = RECONCILE_INTERVAL_MS)
    public synchronized void reconcile() {
        String time = configuredTime();
        if (time.equals(fingerprint) && job != null) {
            return; // inalterado
        }
        cancel();
        register(time);
    }

    /**
     * Lê a hora configurada, com validação e fallback: ausente = default;
     * presente mas inválida = default + warning (a configuração errada não
     * pode silenciar os lembretes nem quebrar o agendamento).
     */
    private String configuredTime() {
        String value = configurationService.get(AppConfigurationService.KEY_CARDS_REMINDER_TIME).orElse(null);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIME;
        }
        String trimmed = value.trim();
        if (!HHMM.matcher(trimmed).matches()) {
            log.warn("Valor inválido para {}: '{}' — usando default {}.",
                    AppConfigurationService.KEY_CARDS_REMINDER_TIME, value, DEFAULT_TIME);
            return DEFAULT_TIME;
        }
        return trimmed;
    }

    private void register(String time) {
        String[] parts = time.split(":");
        // Cron Spring: segundo minuto hora dia-do-mês mês dia-da-semana
        String cron = "0 %s %s * * *".formatted(parts[1], parts[0]);
        try {
            job = integrationTaskScheduler.schedule(
                    reminderJob::sendDueReminders,
                    triggerContext -> new CronTrigger(cron, ZONE).nextExecution(triggerContext));
            fingerprint = time;
            log.info("Lembretes de cards agendados para {} diariamente (cron={}, zone={})", time, cron, ZONE);
        } catch (IllegalArgumentException e) {
            log.error("Falha ao registrar o agendamento dos lembretes (cron={}): {}", cron, e.getMessage());
        }
    }

    private void cancel() {
        if (job != null) {
            job.cancel(false);
            job = null;
        }
        fingerprint = null;
    }
}