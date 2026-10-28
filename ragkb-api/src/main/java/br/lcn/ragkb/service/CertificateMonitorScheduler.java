package br.lcn.ragkb.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
public class CertificateMonitorScheduler {
 private final CertificateMonitorService service;
 @Scheduled(cron="0 * * * * *",zone="America/Sao_Paulo") public void run(){service.dispatchDue();}
}
