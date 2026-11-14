package br.lcn.ragkb.shared.mail;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envia mensagens de e-mail com validação estrita e retry apenas para falhas
 * transitórias de conexão. Cada tentativa usa um sender local, com suas
 * propriedades SMTP próprias; o bean compartilhado nunca é alterado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MILLIS = {1_000, 2_000};
    private static final String MISSING_FROM = "Remetente de e-mail não configurado. "
            + "Defina app.ticket.from-email (ou SMTP_FROM) com um endereço válido.";

    private final JavaMailSenderImpl mailSender;

    @Value("${app.ticket.from-email:}")
    private String fromEmail;

    public void send(String to, String subject, String body) {
        send(to == null ? null : List.of(to), subject, body);
    }

    public void send(List<String> to, String subject, String body) {
        InternetAddress from = validateFrom();
        List<InternetAddress> recipients = validateRecipients(to);
        String recipientSummary = recipients.stream().map(Address::toString).toList().toString();
        Exception lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sendOnce(from, recipients, subject, body);
                log.info("E-mail enviado: to={}, subject={}, tentativa={}", recipientSummary, subject, attempt);
                return;
            } catch (Exception e) {
                lastFailure = e;
                if (!isRetryable(e) || attempt == MAX_ATTEMPTS) {
                    String message = "Falha ao enviar e-mail para " + recipientSummary
                            + " após " + attempt + " tentativa(s).";
                    log.error("{} Assunto='{}', causa={}", message, subject, e.toString(), e);
                    throw new EmailDeliveryException(message, e);
                }

                log.warn("Falha transitória ao enviar e-mail (tentativa {}/{}): {}",
                        attempt, MAX_ATTEMPTS, e.toString());
                try {
                    Thread.sleep(BACKOFF_MILLIS[attempt - 1]);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    String message = "Envio de e-mail interrompido após " + attempt + " tentativa(s).";
                    log.error("{} Destinatários={}, assunto={}", message, recipientSummary, subject, interrupted);
                    throw new EmailDeliveryException(message, interrupted);
                }
            }
        }

        // O loop sempre retorna ou lança; mantém o compilador e a causa explícitos.
        throw new EmailDeliveryException("Falha ao enviar e-mail.", lastFailure);
    }

    private InternetAddress validateFrom() {
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            throw new EmailDeliveryException(MISSING_FROM);
        }
        return validateAddress(fromEmail.trim(), "remetente");
    }

    private List<InternetAddress> validateRecipients(List<String> to) {
        if (to == null || to.isEmpty()) {
            throw new EmailDeliveryException("A lista de destinatários de e-mail está vazia.");
        }
        List<InternetAddress> result = new ArrayList<>(to.size());
        for (String address : to) {
            if (address == null) {
                throw new EmailDeliveryException("Endereço de destinatário inválido: null.");
            }
            result.add(validateAddress(address.trim(), "destinatário"));
        }
        return result;
    }

    private InternetAddress validateAddress(String address, String role) {
        try {
            InternetAddress parsed = new InternetAddress(address, true);
            parsed.validate();
            return parsed;
        } catch (AddressException e) {
            throw new EmailDeliveryException("Endereço de " + role + " inválido: '" + address + "'.", e);
        }
    }

    private void sendOnce(InternetAddress from, List<InternetAddress> recipients,
            String subject, String body) throws MessagingException {
        JavaMailSenderImpl sender = senderWithTimeouts();
        MimeMessage message = sender.createMimeMessage();
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, recipients.toArray(InternetAddress[]::new));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        sender.send(message);
    }

    private JavaMailSenderImpl senderWithTimeouts() {
        Properties properties = new Properties();
        properties.putAll(mailSender.getJavaMailProperties());
        properties.setProperty("mail.smtp.connectiontimeout", "10000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailSender.getHost());
        sender.setPort(mailSender.getPort());
        sender.setProtocol(mailSender.getProtocol());
        sender.setUsername(mailSender.getUsername());
        sender.setPassword(mailSender.getPassword());
        sender.setDefaultEncoding(mailSender.getDefaultEncoding());
        sender.setJavaMailProperties(properties);
        return sender;
    }

    private boolean isRetryable(Exception failure) {
        if (hasCauseNamed(failure, "MailConnectException")) {
            return true;
        }
        if (failure instanceof MailSendException
                && (hasCause(failure, SocketTimeoutException.class)
                || hasCause(failure, ConnectException.class)
                || hasCause(failure, UnknownHostException.class))) {
            return true;
        }
        return hasCause(failure, MessagingException.class) && hasCause(failure, SocketException.class);
    }

    private boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCauseNamed(Throwable failure, String simpleName) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (simpleName.equals(current.getClass().getSimpleName())) {
                return true;
            }
        }
        return false;
    }
}
