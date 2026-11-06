package br.lcn.ragkb.controller;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.lcn.ragkb.dto.CardAttachmentDto;
import br.lcn.ragkb.entity.CardAttachment;
import br.lcn.ragkb.exception.CardNotFoundException;
import br.lcn.ragkb.repository.CardAttachmentRepository;
import br.lcn.ragkb.service.CardAttachmentService;
import br.lcn.ragkb.service.CardService;
import lombok.RequiredArgsConstructor;

/**
 * Escopo pessoal: sem @PreAuthorize — o SecurityConfig já exige autenticação
 * para qualquer request (anyRequest().authenticated()).
 *
 * ACESSO (V20): todo endpoint chama CardService.requireAccessibleCard ANTES de
 * delegar — fonte única da regra (criador do card OU participante de projeto
 * compartilhado; 404 em violação). Fecha a lacuna pré-V20 em que anexos de
 * qualquer card eram operáveis por qualquer usuário autenticado conhecendo o
 * id. O CardAttachmentService não pode depender do CardService (ciclo de
 * dependência), por isso o gate vive no controller.
 */
@RestController
@RequestMapping("/api/cards/{cardId}/attachments")
@RequiredArgsConstructor
public class CardAttachmentController {

    private final CardAttachmentService attachmentService;
    private final CardAttachmentRepository attachmentRepository;
    private final CardService cardService;

    @PostMapping
    public List<CardAttachmentDto> upload(@PathVariable String cardId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        cardService.requireAccessibleCard(cardId, authentication.getName());
        attachmentService.upload(cardId, file);
        return toDtos(attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId));
    }

    @GetMapping
    public List<CardAttachmentDto> list(@PathVariable String cardId,
            Authentication authentication) {
        cardService.requireAccessibleCard(cardId, authentication.getName());
        return toDtos(attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable String cardId,
            @PathVariable String attachmentId,
            Authentication authentication) {
        cardService.requireAccessibleCard(cardId, authentication.getName());

        CardAttachment attachment = attachmentRepository
                .findByCardIdAndId(cardId, attachmentId)
                .orElseThrow(() -> new CardNotFoundException(attachmentId));

        Path file = attachmentService.resolveStorage(attachment);
        String safeName = sanitizeFileName(attachment.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(safeName).build().toString())
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{attachmentId}")
    public List<CardAttachmentDto> delete(@PathVariable String cardId,
            @PathVariable String attachmentId,
            Authentication authentication) {
        cardService.requireAccessibleCard(cardId, authentication.getName());
        attachmentService.delete(cardId, attachmentId);
        return toDtos(attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId));
    }

    private List<CardAttachmentDto> toDtos(List<CardAttachment> attachments) {
        return attachments.stream().map(CardAttachmentDto::from).toList();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\p{Cntrl}\"\\\\/]", "_");
    }
}
