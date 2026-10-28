package br.lcn.ragkb.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="ssl_certificate_check_history") @Getter @NoArgsConstructor
public class CertificateCheck {
    @Id private String id;
    @Column(name="monitor_id",nullable=false) private String monitorId;
    @Column(name="checked_at",nullable=false) private Instant checkedAt;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    @Column(name="days_remaining") private Integer daysRemaining;
    @Column(name="expires_at") private Instant expiresAt;
    @Column(name="checked_url",length=2048) private String checkedUrl;
    @Column(name="error_message",columnDefinition="TEXT") private String errorMessage;
    @Column(name="alert_sent",nullable=false) private boolean alertSent;
    public enum Status { VALID, EXPIRING, EXPIRED, INVALID_CERTIFICATE, CONNECTION_ERROR }
    public CertificateCheck(String monitorId, Instant checkedAt, Status status, Integer daysRemaining, Instant expiresAt, String url, String error, boolean alertSent) {
        this.id=UUID.randomUUID().toString(); this.monitorId=monitorId; this.checkedAt=checkedAt; this.status=status;
        this.daysRemaining=daysRemaining; this.expiresAt=expiresAt; this.checkedUrl=url; this.errorMessage=error; this.alertSent=alertSent;
    }
}
