package br.lcn.ragkb.ticket;

import java.util.Map;

import br.lcn.ragkb.shared.mail.EmailDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class TicketExceptionHandler {

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(EmailDeliveryException e) {
        log.error("Falha ao enviar chamado por e-mail: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "Falha ao enviar o chamado por e-mail. Tente novamente em instantes.",
                        "detail", e.getMessage() == null ? "" : e.getMessage()));
    }
}
