package br.lcn.ragkb.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.lcn.ragkb.dto.WhatsAppStatusResponse;
import br.lcn.ragkb.dto.WhatsAppTestSendRequest;
import br.lcn.ragkb.whatsapp.WhatsAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de operação/fumaça do canal WhatsApp (WAHA). Restritos a ADMIN:
 * disparam envios reais para o número dedicado.
 */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @GetMapping("/status")
    public WhatsAppStatusResponse status() {
        String rawStatus = whatsAppService.sessionStatus();
        return new WhatsAppStatusResponse("WORKING".equalsIgnoreCase(rawStatus), rawStatus);
    }

    @PostMapping("/session/start")
    public Map<String, String> startSession() {
        whatsAppService.startSession();
        return Map.of("message",
                "Sessão iniciada. Escaneie o QR Code para emparelhar.");
    }

    /**
     * QR de emparelhamento em base64. 409 se a sessão já está conectada (não há
     * QR a exibir) — a UI decide o que mostrar nesse caso.
     */
    @GetMapping("/qr")
    public ResponseEntity<?> qr() {
        return whatsAppService.getQrCode()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(409)
                .body(Map.of("message", "Sessão já conectada — não há QR Code a exibir.")));
    }

    @PostMapping("/test-send")
    public Map<String, String> testSend(@Valid @RequestBody WhatsAppTestSendRequest request) {
        whatsAppService.sendText(request.chatId(), request.message());
        return Map.of("message", "Mensagem enviada com sucesso.");
    }
}
