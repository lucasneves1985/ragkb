package br.lcn.ragkb.ticket;

import br.lcn.ragkb.shared.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTicketGateway implements TicketGateway {

    private final EmailService emailService;

    @Value("${app.ticket.recipient-email}")
    private String recipientEmail;

    @Override
    public String openTicket(TicketSuggestion suggestion) {
        String subject = "[Chamado técnico] " + suggestion.question();
        String body = """
                Solicitante: %s
                Assunto: %s

                Descrição:
                %s
                """.formatted(suggestion.userId(), suggestion.question(), suggestion.description());

        emailService.send(recipientEmail, subject, body);

        log.info("Chamado enviado por e-mail: to={}, subject={}", recipientEmail, subject);
        return "EMAIL-" + suggestion.id();
    }
}
