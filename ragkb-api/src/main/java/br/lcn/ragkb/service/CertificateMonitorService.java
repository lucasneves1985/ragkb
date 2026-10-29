package br.lcn.ragkb.service;

import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.lcn.ragkb.dto.*;
import br.lcn.ragkb.entity.*;
import br.lcn.ragkb.repository.*;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class CertificateMonitorService {
    public static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter EXPIRY_DATE_FORMAT=DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONE);
    private final CertificateMonitorRepository monitors;
    private final CertificateCheckRepository history;
    private final WhatsAppService whatsApp;
    private final JavaMailSender mailSender;
    @Value("${app.ticket.from-email:}") private String fromEmail;

    @Transactional(readOnly=true) public List<CertificateMonitorDto> list(String user){return monitors.findByCreatedByOrderByCreatedAtDesc(user).stream().map(CertificateMonitorDto::from).toList();}
    @Transactional public CertificateMonitorDto create(CertificateMonitorRequest r,String user){var m=monitors.save(new CertificateMonitor(normalize(r.url()),LocalTime.parse(r.checkTime()),r.alertThresholdDays(),weekdays(r.alertWeekdays()),r.notificationChannel(),target(r),user));return CertificateMonitorDto.from(m);}
    @Transactional public CertificateMonitorDto update(String id,CertificateMonitorRequest r,String user){var m=find(id,user);m.update(normalize(r.url()),LocalTime.parse(r.checkTime()),r.alertThresholdDays(),weekdays(r.alertWeekdays()),r.notificationChannel(),target(r),r.active());return CertificateMonitorDto.from(monitors.save(m));}
    @Transactional public void delete(String id,String user){monitors.delete(find(id,user));}
    @Transactional(readOnly=true) public List<CertificateCheckDto> history(String id,String user){find(id,user);return history.findTop100ByMonitorIdOrderByCheckedAtDesc(id).stream().map(CertificateCheckDto::from).toList();}
    private CertificateMonitor find(String id,String user){return monitors.findByIdAndCreatedBy(id,user).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Monitor de certificado não encontrado."));}
    private String target(CertificateMonitorRequest r){String t=r.notificationTarget().trim();if(r.notificationChannel()==CertificateMonitor.Channel.EMAIL&&!t.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new IllegalArgumentException("Informe um e-mail válido.");if(r.notificationChannel()==CertificateMonitor.Channel.WHATSAPP&&t.replaceAll("\\D","").length()<10)throw new IllegalArgumentException("Informe um telefone WhatsApp válido.");return t;}
    private String weekdays(List<String> days){try{return days.stream().map(s->DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT)).name()).distinct().sorted().reduce((a,b)->a+","+b).orElseThrow();}catch(Exception e){throw new IllegalArgumentException("Dias da semana inválidos. Use MONDAY a SUNDAY.");}}
    private String normalize(String raw){try{String value=raw.trim();if(!value.matches("(?i)^https?://.*"))value="https://"+value;URI u=new URI(value).normalize();if(u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException("Informe uma URL válida.");String path=u.getRawPath();while(path!=null&&!path.isEmpty()&&path.endsWith("/"))path=path.substring(0,path.length()-1);return u.getScheme().toLowerCase()+"://"+u.getRawAuthority()+(path==null?"":path)+(u.getRawQuery()==null?"":"?"+u.getRawQuery());}catch(URISyntaxException e){throw new IllegalArgumentException("Informe uma URL válida.");}}

    /** Chamado a cada minuto; a data persistida permite recuperar disparos perdidos após reinício. */
    @Transactional
    public void dispatchDue(){LocalDate today=LocalDate.now(ZONE);LocalTime now=LocalTime.now(ZONE).withSecond(0).withNano(0);
      for(var candidate:monitors.findByActiveTrue())try{if(LocalTime.parse(candidate.getCheckTime()).isAfter(now)||today.equals(candidate.getLastCheckLocalDate()))continue;
        var m=monitors.findFirstById(candidate.getId()).orElse(null);if(m==null||!m.isActive()||today.equals(m.getLastCheckLocalDate()))continue;
        m.markChecked(Instant.now(),today);monitors.saveAndFlush(m);check(m,today);
      }catch(Exception e){log.error("Falha no monitor SSL {}: {}",candidate.getId(),e.getMessage(),e);}
    }
    public void checkNow(String id,String user){var m=find(id,user);log.info("Checagem SSL manual solicitada para monitor {} ({})",m.getId(),m.getUrl());LocalDate today=LocalDate.now(ZONE);check(m,today);}
    private void check(CertificateMonitor m,LocalDate today){Instant checked=Instant.now();String finalUrl=null;CertificateCheck.Status status;Integer days=null;Instant expires=null;String error=null;boolean alert=false;
      try{URI uri=followRedirects(URI.create(m.getUrl()));finalUrl=uri.toString();if(!"https".equalsIgnoreCase(uri.getScheme()))throw new IllegalStateException("O endereço final não utiliza HTTPS.");
        X509Certificate cert=certificate(uri);expires=cert.getNotAfter().toInstant();days=(int)Math.floorDiv(Duration.between(checked,expires).getSeconds(),86_400);status=days<0?CertificateCheck.Status.EXPIRED:days<=m.getAlertThresholdDays()?CertificateCheck.Status.EXPIRING:CertificateCheck.Status.VALID;
        boolean dayAllowed=Arrays.asList(m.getAlertWeekdays().split(",")).contains(today.getDayOfWeek().name());
        alert=days<=m.getAlertThresholdDays()&&dayAllowed;
      }catch(Exception e){status=(e instanceof CertificateExceptionLike)?CertificateCheck.Status.INVALID_CERTIFICATE:CertificateCheck.Status.CONNECTION_ERROR;error=safeMessage(e);alert=true;}
      boolean alreadyAlerted=today.equals(m.getLastAlertLocalDate());
      if(alert&&!alreadyAlerted){try{log.info("Enviando alerta SSL do monitor {} pelo canal {}",m.getId(),m.getNotificationChannel());notify(m,status,days,expires,error,finalUrl);m.markAlerted(today);monitors.save(m);log.info("Alerta SSL enviado para monitor {} pelo canal {}",m.getId(),m.getNotificationChannel());}catch(Exception e){log.warn("Não foi possível enviar alerta SSL do monitor {} pelo canal {}: {}",m.getId(),m.getNotificationChannel(),e.getMessage(),e);alert=false;}}
      else if(!alert)log.info("Alerta SSL não aplicável para monitor {}: status={}, diasRestantes={}, limite={}, dia={}, diasConfigurados={}",m.getId(),status,days,m.getAlertThresholdDays(),today.getDayOfWeek(),m.getAlertWeekdays());
      else log.info("Alerta SSL suprimido para monitor {}: já enviado em {}",m.getId(),today);
      log.info("Checagem SSL concluída para monitor {}: status={}, diasRestantes={}, alertaEnviado={}",m.getId(),status,days,alert&&!alreadyAlerted);
      history.save(new CertificateCheck(m.getId(),checked,status,days,expires,finalUrl,error,alert));
    }
    private URI followRedirects(URI start)throws Exception{URI current=start;for(int i=0;i<=5;i++){if(!Set.of("http","https").contains(current.getScheme().toLowerCase()))throw new IllegalStateException("Esquema de URL não suportado.");HttpURLConnection c=(HttpURLConnection)current.toURL().openConnection();c.setConnectTimeout(10_000);c.setReadTimeout(10_000);c.setInstanceFollowRedirects(false);c.setRequestMethod("HEAD");int code=c.getResponseCode();if(code>=300&&code<400){String loc=c.getHeaderField("Location");c.disconnect();if(loc==null)throw new IllegalStateException("Redirecionamento sem destino.");if(i==5)throw new IllegalStateException("Limite de redirecionamentos excedido.");current=current.resolve(loc);continue;}c.disconnect();return current;}throw new IllegalStateException("Limite de redirecionamentos excedido.");}
    private X509Certificate certificate(URI uri)throws Exception{TrustManager[] trust={new X509TrustManager(){public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}public void checkClientTrusted(X509Certificate[] c,String a){}public void checkServerTrusted(X509Certificate[] c,String a){}}};SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(null,trust,new java.security.SecureRandom());int port=uri.getPort()>0?uri.getPort():443;
      try(SSLSocket socket=(SSLSocket)ctx.getSocketFactory().createSocket()){socket.connect(new InetSocketAddress(uri.getHost(),port),10_000);socket.setSoTimeout(10_000);var params=socket.getSSLParameters();params.setEndpointIdentificationAlgorithm("HTTPS");socket.setSSLParameters(params);socket.startHandshake();var chain=socket.getSession().getPeerCertificates();var cert=(X509Certificate)chain[0];
        // Separar a coleta do certificado da validação de confiança permite registrar
        // a validade mesmo quando o endpoint apresenta uma cadeia autoassinada.
        if(new java.util.Date().after(cert.getNotAfter()))return cert;
        try{var factory=javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());factory.init((java.security.KeyStore)null);for(var manager:factory.getTrustManagers())if(manager instanceof X509TrustManager x){x.checkServerTrusted(Arrays.copyOf(chain,chain.length,X509Certificate[].class),cert.getPublicKey().getAlgorithm());break;}}
        catch(Exception e){throw new CertificateExceptionLike("Cadeia do certificado inválida ou autoassinada: "+safeMessage(e));}
        return cert;}}
    private void notify(CertificateMonitor m,CertificateCheck.Status status,Integer days,Instant expires,String error,String url)throws Exception{String detail=status==CertificateCheck.Status.CONNECTION_ERROR||status==CertificateCheck.Status.INVALID_CERTIFICATE?error:"certificado vence em "+days+" dia(s)"+(expires==null?"":" ("+EXPIRY_DATE_FORMAT.format(expires)+")");String text="Alerta SSL/TLS: %s — %s. %s".formatted(m.getUrl(),status,detail);
      if(m.getNotificationChannel()==CertificateMonitor.Channel.WHATSAPP){String phone=m.getNotificationTarget().replaceAll("\\D","");if(phone.length()<=11&&!phone.startsWith("55"))phone="55"+phone;whatsApp.sendText(phone+"@c.us",text);}else{if(fromEmail==null||fromEmail.isBlank())throw new IllegalStateException("E-mail remetente não configurado.");MimeMessage msg=mailSender.createMimeMessage();MimeMessageHelper helper=new MimeMessageHelper(msg,"UTF-8");helper.setFrom(fromEmail);helper.setTo(m.getNotificationTarget());helper.setSubject("Alerta de certificado SSL/TLS — "+m.getUrl());helper.setText(text);mailSender.send(msg);}}
    private String safeMessage(Exception e){String s=e.getMessage();if(e instanceof UnknownHostException)return "Domínio inexistente ou sem resolução DNS.";if(e instanceof java.net.ConnectException)return "Conexão recusada pelo host.";if(e instanceof java.net.SocketTimeoutException)return "Tempo limite de conexão excedido (10 segundos).";return s==null?"Falha ao verificar certificado.":s.substring(0,Math.min(500,s.length()));}
    private static class CertificateExceptionLike extends Exception{CertificateExceptionLike(String m){super(m);}}
}
