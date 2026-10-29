package br.lcn.ragkb.dto;
import java.time.Instant;
import br.lcn.ragkb.entity.CertificateCheck;
public record CertificateCheckDto(String id,Instant checkedAt,CertificateCheck.Status status,Integer daysRemaining,Instant expiresAt,String checkedUrl,String errorMessage,boolean alertSent) {
 public static CertificateCheckDto from(CertificateCheck c){return new CertificateCheckDto(c.getId(),c.getCheckedAt(),c.getStatus(),c.getDaysRemaining(),c.getExpiresAt(),c.getCheckedUrl(),c.getErrorMessage(),c.isAlertSent());}
}
