package br.lcn.ragkb.controller;

import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.lcn.ragkb.dto.BusinessRuleAttachmentDto;
import br.lcn.ragkb.entity.BusinessRuleAttachment;
import br.lcn.ragkb.exception.BusinessRuleNotFoundException;
import br.lcn.ragkb.repository.BusinessRuleAttachmentRepository;
import br.lcn.ragkb.service.BusinessRuleAttachmentService;
import br.lcn.ragkb.service.BusinessRuleService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/business-rules/{ruleId}/attachments")
@RequiredArgsConstructor
public class BusinessRuleAttachmentController {

    private final BusinessRuleAttachmentService attachmentService;
    private final BusinessRuleService ruleService;
    private final BusinessRuleAttachmentRepository attachmentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<BusinessRuleAttachmentDto> upload(@PathVariable String ruleId,
                                                  @RequestParam("file") MultipartFile file,
                                                  Authentication auth) {
        attachmentService.upload(ruleId, file, auth.getName());
        return toDtos(attachmentRepository.findByRuleId(ruleId));
    }

    /** Visibility mirrors the rule detail (assertReadable) — same gate. */
    @GetMapping
    public List<BusinessRuleAttachmentDto> list(@PathVariable String ruleId, Authentication auth) {
        ruleService.assertReadable(ruleId, auth.getName(), isAdmin(auth));
        return toDtos(attachmentRepository.findByRuleId(ruleId));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable String ruleId,
                                                       @PathVariable String attachmentId,
                                                       Authentication auth) {
        ruleService.assertReadable(ruleId, auth.getName(), isAdmin(auth));
        BusinessRuleAttachment attachment = attachmentRepository
                .findByRuleIdAndId(ruleId, attachmentId)
                .orElseThrow(() -> new BusinessRuleNotFoundException(attachmentId));

        Path file = attachmentService.resolveStorage(attachment);
        String safeName = sanitizeFileName(attachment.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(safeName).build().toString())
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<BusinessRuleAttachmentDto> delete(@PathVariable String ruleId,
                                                  @PathVariable String attachmentId,
                                                  Authentication auth) {
        attachmentService.delete(ruleId, attachmentId);
        return toDtos(attachmentRepository.findByRuleId(ruleId));
    }

    private List<BusinessRuleAttachmentDto> toDtos(List<BusinessRuleAttachment> attachments) {
        return attachments.stream().map(BusinessRuleAttachmentDto::from).toList();
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\p{Cntrl}\"\\\\/]", "_");
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}