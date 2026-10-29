package br.lcn.ragkb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * Scheduler dinâmico para as integrações SCHEDULED (registro/desregistro em
     * runtime pelo IntegrationScheduler). Pool dedicado — não disputa threads
     * com o @Scheduled de reconciliação.
     */
    @Bean
    public ThreadPoolTaskScheduler integrationTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("integration-sched-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
