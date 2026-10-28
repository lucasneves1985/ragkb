package br.lcn.ragkb.dto;
import java.util.List;
import br.lcn.ragkb.entity.CertificateMonitor.Channel;
import jakarta.validation.constraints.*;
public record CertificateMonitorRequest(@NotBlank @Size(max=2048) String url,
 @NotBlank @Pattern(regexp="^([01]\\d|2[0-3]):[0-5]\\d$") String checkTime,
 @Min(0) @Max(365) int alertThresholdDays, @NotEmpty List<@NotBlank String> alertWeekdays,
 @NotNull Channel notificationChannel, @NotBlank @Size(max=255) String notificationTarget, boolean active) {}
