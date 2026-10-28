package br.lcn.ragkb.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ssl_certificate_monitors")
@Getter
@NoArgsConstructor
public class CertificateMonitor {
    @Id private String id;
    @Column(nullable=false, length=2048) private String url;
    @Column(name="check_time", nullable=false, length=5) private String checkTime;
    @Column(name="alert_threshold_days", nullable=false) private int alertThresholdDays;
    @Column(name="alert_weekdays", nullable=false, length=40) private String alertWeekdays;
    @Enumerated(EnumType.STRING) @Column(name="notification_channel", nullable=false, length=20) private Channel notificationChannel;
    @Column(name="notification_target", nullable=false, length=255) private String notificationTarget;
    @Column(nullable=false) private boolean active=true;
    @Column(name="created_by",nullable=false,length=100) private String createdBy;
    @Column(name="last_checked_at") private Instant lastCheckedAt;
    @Column(name="last_check_local_date") private LocalDate lastCheckLocalDate;
    @Column(name="last_alert_local_date") private LocalDate lastAlertLocalDate;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    public enum Channel { EMAIL, WHATSAPP }
    public CertificateMonitor(String url, LocalTime time, int threshold, String weekdays, Channel channel, String target, String username) {
        this.id=UUID.randomUUID().toString(); this.url=url; this.checkTime=time.toString(); this.alertThresholdDays=threshold;
        this.alertWeekdays=weekdays; this.notificationChannel=channel; this.notificationTarget=target; this.createdBy=username;
    }
    public void update(String url, LocalTime time, int threshold, String weekdays, Channel channel, String target, boolean active) {
        this.url=url; this.checkTime=time.toString(); this.alertThresholdDays=threshold; this.alertWeekdays=weekdays;
        this.notificationChannel=channel; this.notificationTarget=target; this.active=active;
    }
    public void markChecked(Instant instant, LocalDate date) { lastCheckedAt=instant; lastCheckLocalDate=date; }
    public void markAlerted(LocalDate date) { lastAlertLocalDate=date; }
    @PrePersist void createTimestamps(){createdAt=Instant.now();updatedAt=createdAt;}
    @PreUpdate void updateTimestamp(){updatedAt=Instant.now();}
}
