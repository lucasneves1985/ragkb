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
 * Agendamento dinâmico do CardDigestJob a partir das configurações globais
 * (app_configuration — editáveis apenas por ADMIN):
 * - cards.digest_enabled ("true"/"false", default FALSE)
 * - cards.digest_frequency (DAILY | WEEKLY, default DAILY)
 * - cards.digest_time (HH:mm, default 07:30 — 30 min antes do lembrete de
 * prazo para não empilhar duas mensagens no mesmo minuto)
 * - cards.digest_weekday (MON..SUN, default MON — usado apenas no WEEKLY)
 *
 * Modelo de reconciliação — MESMO padrão do CardReminderScheduler e do
 * IntegrationScheduler: a cada 30s compara os valores resolvidos com o job
 * registrado e cancela/registra o que mudou. Desligar o digest cancela o job
 * (nenhuma execução fantasma). Mudança vale em até 30s, sem restart; sobrevive
 * a restart via @PostConstruct.
 *
 * Validação com fallback: valor ausente/inválido usa o default com warning —
 * configuração errada nunca derruba o agendamento nem silencia o digest
 * silenciosamente (o warning fica no log para o admin investigar).
 *
 * Limitação aceita: single-instância (mesma do IntegrationScheduler).
 */
@Service
@RequiredArgsConstructor
public class CardDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(CardDigestScheduler.class);
    private static final long RECONCILE_INTERVAL_MS = 30_000;
    private static final String DEFAULT_TIME = "07:30";
    private static final String DEFAULT_FREQUENCY = "DAILY";
    private static final String DEFAULT_WEEKDAY = "MON";
    private static final Pattern HHMM = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    private static final Pattern WEEKDAY = Pattern.compile("^(MON|TUE|WED|THU|FRI|SAT|SUN)$");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final TaskScheduler integrationTaskScheduler;
    private final AppConfigurationService configurationService;
    private final CardDigestJob digestJob;

    private ScheduledFuture<?> job;
    private String fingerprint;

    @PostConstruct
    void reconcileOnStartup() {
        reconcile();
    }

    @Scheduled(fixedDelay = RECONCILE_INTERVAL_MS)
    public synchronized void reconcile() {
        boolean enabled = enabled();
        String time = time();
        String frequency = frequency();
        String weekday = weekday();

        String fp = "%s|%s|%s|%s".formatted(enabled, frequency, time, weekday);
        if (fp.equals(fingerprint) && job != null) {
            return; // inalterado
        }
        if (fp.equals(fingerprint) && job == null && !enabled) {
            return; // desligado e já registrado como desligado
        }
        cancel();
        fingerprint = fp;

        if (!enabled) {
            log.info("Digest de tarefas desativado (cards.digest_enabled).");
            return;
        }

        String cron = cronFor(frequency, time, weekday);
        try {
            job = integrationTaskScheduler.schedule(
                    digestJob::sendDigest,
                    triggerContext -> new CronTrigger(cron, ZONE).nextExecution(triggerContext));
            log.info("Digest de tarefas agendado: frequência={}, horário={}{}, cron={}, zone={}",
                    frequency, time, "WEEKLY".equals(frequency) ? ", dia=" + weekday : "", cron, ZONE);
        } catch (IllegalArgumentException e) {
            log.error("Falha ao registrar o agendamento do digest (cron={}): {}", cron, e.getMessage());
        }
    }

    private boolean enabled() {
        return "true".equalsIgnoreCase(
                configurationService.get(AppConfigurationService.KEY_CARDS_DIGEST_ENABLED).orElse(null));
    }

    private String time() {
        String value = configurationService.get(AppConfigurationService.KEY_CARDS_DIGEST_TIME).orElse(null);
        if (value == null || value.isBlank()) {
            return DEFAULT_TIME;
        }
        String trimmed = value.trim();
        if (!HHMM.matcher(trimmed).matches()) {
            log.warn("Valor inválido para {}: '{}' — usando default {}.",
                    AppConfigurationService.KEY_CARDS_DIGEST_TIME, value, DEFAULT_TIME);
            return DEFAULT_TIME;
        }
        return trimmed;
    }

    private String frequency() {
        String value = configurationService.get(AppConfigurationService.KEY_CARDS_DIGEST_FREQUENCY).orElse(null);
        if (value == null || value.isBlank()) {
            return DEFAULT_FREQUENCY;
        }
        String trimmed = value.trim().toUpperCase();
        if (!trimmed.equals("DAILY") && !trimmed.equals("WEEKLY")) {
            log.warn("Valor inválido para {}: '{}' — usando default {}.",
                    AppConfigurationService.KEY_CARDS_DIGEST_FREQUENCY, value, DEFAULT_FREQUENCY);
            return DEFAULT_FREQUENCY;
        }
        return trimmed;
    }

    private String weekday() {
        String value = configurationService.get(AppConfigurationService.KEY_CARDS_DIGEST_WEEKDAY).orElse(null);
        if (value == null || value.isBlank()) {
            return DEFAULT_WEEKDAY;
        }
        String trimmed = value.trim().toUpperCase();
        if (!WEEKDAY.matcher(trimmed).matches()) {
            log.warn("Valor inválido para {}: '{}' — usando default {}.",
                    AppConfigurationService.KEY_CARDS_DIGEST_WEEKDAY, value, DEFAULT_WEEKDAY);
            return DEFAULT_WEEKDAY;
        }
        return trimmed;
    }

    /**
     * Cron Spring (segundo minuto hora dia mês dia-da-semana). O dia da
     * semana usa o nome (MON..SUN), aceito nativamente pelo CronTrigger.
     */
    private String cronFor(String frequency, String time, String weekday) {
        String[] parts = time.split(":");
        if ("WEEKLY".equals(frequency)) {
            return "0 %s %s * * %s".formatted(parts[1], parts[0], weekday);
        }
        return "0 %s %s * * *".formatted(parts[1], parts[0]);
    }

    private void cancel() {
        if (job != null) {
            job.cancel(false);
            job = null;
        }
    }
}