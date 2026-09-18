package br.lcn.ragkb.dto;

import java.time.Instant;

import br.lcn.ragkb.entity.BusinessRuleAttachment;

/**
 * Contract for rule attachments in JSON responses. Exists because the
 * entity holds a lazy ManyToOne to BusinessRule and open-in-view is off:
 * serializing the entity would hit the proxy after the session closes
 * (LazyInitializationException -> 500 on list/upload/delete) and would
 * leak the internal storagePath. Only what the UI consumes goes out.
 */
public record BusinessRuleAttachmentDto(
        String id,
        String fileName,
        String contentType,
        Long sizeBytes,
        String uploadedBy,
        Instant uploadedAt) {

    public static BusinessRuleAttachmentDto from(BusinessRuleAttachment a) {
        return new BusinessRuleAttachmentDto(
                a.getId(),
                a.getFileName(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getUploadedBy(),
                a.getUploadedAt());
    }
}