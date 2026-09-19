package br.lcn.ragkb.service;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationType;
import br.lcn.ragkb.repository.IntegrationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Registro dinâmico de jobs das integrações SCHEDULED.
 *
 * Modelo de reconciliação: a cada 30s compara os jobs registrados com o banco e
 * cancela/registra o que mudou. Vantagem sobre eventos de CRUD: não depende de
 * hooks no IntegrationService, sobrevive a restart (reconcilia no boot) e nunca
 * fica com job órfão. Trade-off: mudança de agendamento leva até 30s para
 * valer.
 *
 * Limitação aceita: single-instância. Em múltiplas instâncias, aplicar ShedLock
 * na execução para evitar disparo duplicado.
 */
@Service
@RequiredArgsConstructor
public class IntegrationScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationScheduler.class);
    private static final long RECONCILE_INTERVAL_MS = 30_000;

    private final TaskScheduler integrationTaskScheduler;
    private final IntegrationRepository repository;
    private final IntegrationExecutor executor;

    private final Map<String, ScheduledFuture<?>> jobs = new ConcurrentHashMap<>();
    private final Map<String, String> fingerprints = new ConcurrentHashMap<>();

    @PostConstruct
    void reconcileOnStartup() {
        reconcile();
    }

    @Scheduled(fixedDelay = RECONCILE_INTERVAL_MS)
    public void reconcile() {
        var scheduled = repository.findByIntegrationTypeAndActiveTrue(IntegrationType.SCHEDULED);
        var activeIds = scheduled.stream().map(Integration::getId).collect(java.util.stream.Collectors.toSet());

        // Desregistra jobs que saíram do ar ou foram deletados
        jobs.keySet().removeIf(id -> {
            if (!activeIds.contains(id)) {
                cancelQuietly(id);
                return true;
            }
            return false;
        });

        for (Integration integration : scheduled) {
            String fingerprint = fingerprint(integration);
            String current = fingerprints.get(integration.getId());
            if (fingerprint.equals(current) && jobs.containsKey(integration.getId())) {
                continue; // inalterado
            }
            cancelQuietly(integration.getId());
            register(integration, fingerprint);
        }
    }

    private void register(Integration integration, String fingerprint) {
        try {
            var future = integrationTaskScheduler.schedule(
                    () -> runIntegration(integration.getId()), triggerFor(integration));
            jobs.put(integration.getId(), future);
            fingerprints.put(integration.getId(), fingerprint);
            log.info("Job registrado: '{}' ({}), fingerprint={}", integration.getName(),
                    describeTrigger(integration), fingerprint);
        } catch (IllegalArgumentException e) {
            // Cron inválido cadastrado direto no banco (fora da validação do service)
            log.error("Falha ao registrar job da integração '{}': {}", integration.getName(), e.getMessage());
        }
    }

    private void runIntegration(String id) {
        // Lê a integração fresca do banco a cada execução — nunca usa estado em cache
        repository.findById(id)
                .filter(Integration::isActive)
                .filter(i -> i.getIntegrationType() == IntegrationType.SCHEDULED)
                .ifPresent(executor::execute);
    }

    private org.springframework.scheduling.Trigger triggerFor(Integration integration) {
        if (integration.getScheduleCron() != null && !integration.getScheduleCron().isBlank()) {
            ZoneId zone = ZoneId.of(integration.getScheduleTimezone());
            return triggerContext -> new CronTrigger(integration.getScheduleCron(), zone)
                    .nextExecution(triggerContext);
        }
        var periodic = new PeriodicTrigger(java.time.Duration.ofSeconds(integration.getScheduleIntervalSeconds()));
        return triggerContext -> periodic.nextExecution(triggerContext);
    }

    private String fingerprint(Integration integration) {
        return "%s|%s|%s|%s".formatted(
                integration.getScheduleCron() == null ? "" : integration.getScheduleCron(),
                integration.getScheduleTimezone(),
                integration.getScheduleIntervalSeconds() == null ? "" : integration.getScheduleIntervalSeconds(),
                integration.isActive());
    }

    private String describeTrigger(Integration integration) {
        if (integration.getScheduleCron() != null && !integration.getScheduleCron().isBlank()) {
            return "cron=" + integration.getScheduleCron() + "@" + integration.getScheduleTimezone();
        }
        return "interval=" + integration.getScheduleIntervalSeconds() + "s";
    }

    private void cancelQuietly(String id) {
        var future = jobs.remove(id);
        if (future != null) {
            future.cancel(false);
        }
        fingerprints.remove(id);
        log.debug("Job desregistrado: {}", id);
    }
}
