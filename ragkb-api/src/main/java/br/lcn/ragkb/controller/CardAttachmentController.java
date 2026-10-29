package br.lcn.ragkb.controller;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import lombok.RequiredArgsConstructor;

/**
 * Escopo pessoal: sem @PreAuthorize — o SecurityConfig já exige autenticação
 * para qualquer request (anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/cards/{cardId}/attachments")
@RequiredArgsConstructor
public class CardAttachmentController {

    private final CardAttachmentService attachmentService;
    private final CardAttachmentRepository attachmentRepository;

    @PostMapping
    public List<CardAttachmentDto> upload(@PathVariable String cardId,
            @RequestParam("file") MultipartFile file) {
        attachmentService.upload(cardId, file);
        return toDtos(attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId));
    }

    @GetMapping
    public List<CardAttachmentDto> list(@PathVariable String cardId) {
        return toDtos(attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable String cardId,
            @PathVariable String attachmentId) {
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
            @PathVariable String attachmentId) {
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
