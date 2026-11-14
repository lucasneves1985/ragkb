package br.lcn.ragkb.whatsapp;



import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.lcn.ragkb.whatsapp.api.WhatsAppNotConnectedException;
import br.lcn.ragkb.whatsapp.api.WhatsAppSendException;

@RestControllerAdvice
public class WhatsAppExceptionHandler {

    @ExceptionHandler(WhatsAppNotConnectedException.class)
    public ResponseEntity<Map<String, String>> handleWhatsAppNotConnected(WhatsAppNotConnectedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(WhatsAppSendException.class)
    public ResponseEntity<Map<String, String>> handleWhatsAppSend(WhatsAppSendException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }
}
