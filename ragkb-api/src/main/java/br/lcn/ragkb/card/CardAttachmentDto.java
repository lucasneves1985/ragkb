package br.lcn.ragkb.card;



import java.time.Instant;

public record CardAttachmentDto(
        String id,
        String fileName,
        String contentType,
        Long sizeBytes,
        Instant uploadedAt) {

    public static CardAttachmentDto from(CardAttachment a) {
        return new CardAttachmentDto(a.getId(), a.getFileName(),
                a.getContentType(), a.getSizeBytes(), a.getUploadedAt());
    }
}
