package br.lcn.ragkb.gateway;

import br.lcn.ragkb.dto.TicketSuggestion;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTicketGateway implements TicketGateway {

    private final JavaMailSender mailSender;

    @Value("${app.ticket.recipient-email}")
    private String recipientEmail;

    @Value("${app.ticket.from-email:}")
    private String fromEmail;

    @Override
    public String openTicket(TicketSuggestion suggestion) {
        String subject = "[Chamado técnico] " + suggestion.question();
        String body = """
                Solicitante: %s
                Assunto: %s

                Descrição:
                %s
                """.formatted(suggestion.userId(), suggestion.question(), suggestion.description());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            if (!fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }
            message.setRecipients(Message.RecipientType.TO, recipientEmail);
            message.setSubject(subject);
            message.setText(body, "UTF-8");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Falha ao enviar e-mail de chamado técnico", e);
        }

        log.info("Chamado enviado por e-mail: to={}, subject={}", recipientEmail, subject);
        return "EMAIL-" + suggestion.id();
    }
}