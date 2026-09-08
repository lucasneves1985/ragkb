package br.lcn.ragkb.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

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
}