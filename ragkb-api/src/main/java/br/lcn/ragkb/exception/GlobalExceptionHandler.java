package br.lcn.ragkb.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.lcn.ragkb.whatsapp.WhatsAppNotConnectedException;
import br.lcn.ragkb.whatsapp.WhatsAppSendException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SectorNameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleSectorNameExists(SectorNameAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(SectorInUseException.class)
    public ResponseEntity<Map<String, String>> handleSectorInUse(SectorInUseException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(SectorNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSectorNotFound(SectorNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleArticleNotFoundException(ArticleNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(InvalidArticleContentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArticleContentException(InvalidArticleContentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleNotFound(BusinessRuleNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateBusinessRuleTitleException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateBusinessRuleTitle(DuplicateBusinessRuleTitleException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IntegrationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleIntegrationNotFound(IntegrationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateIntegrationNameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateIntegrationName(DuplicateIntegrationNameException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(InvalidIntegrationException.class)
    public ResponseEntity<Map<String, String>> handleInvalidIntegration(InvalidIntegrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(WhatsAppNotConnectedException.class)
    public ResponseEntity<Map<String, String>> handleWhatsAppNotConnected(WhatsAppNotConnectedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(WhatsAppSendException.class)
    public ResponseEntity<Map<String, String>> handleWhatsAppSend(WhatsAppSendException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
    }
}