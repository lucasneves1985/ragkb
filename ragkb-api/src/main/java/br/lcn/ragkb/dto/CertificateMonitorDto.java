package br.lcn.ragkb.dto;
import java.time.Instant;
import java.util.List;
import br.lcn.ragkb.entity.CertificateMonitor;
public record CertificateMonitorDto(String id,String url,String checkTime,int alertThresholdDays,List<String> alertWeekdays,
 CertificateMonitor.Channel notificationChannel,String notificationTarget,boolean active,Instant lastCheckedAt) {
 public static CertificateMonitorDto from(CertificateMonitor m){return new CertificateMonitorDto(m.getId(),m.getUrl(),m.getCheckTime(),m.getAlertThresholdDays(),List.of(m.getAlertWeekdays().split(",")),m.getNotificationChannel(),m.getNotificationTarget(),m.isActive(),m.getLastCheckedAt());}
}
