package br.lcn.ragkb.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardAttachment;
import br.lcn.ragkb.exception.CardNotFoundException;
import br.lcn.ragkb.repository.CardAttachmentRepository;
import br.lcn.ragkb.repository.CardRepository;
import jakarta.annotation.PostConstruct;

/**
 * Espelho do BusinessRuleAttachmentService, com duas diferenças de escopo: sem
 * uploadedBy (projeto pessoal) e storage COMPARTILHADO com os anexos de regras
 * (ragkb.media.attachments-path) — nomes são UUID, sem colisão.
 */
@Service
public class CardAttachmentService {

    private static final long MAX_SIZE_BYTES = 20 * 1024 * 1024;

    /**
     * extension → content type emitted on download (whitelist-driven).
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "csv", "text/csv",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "zip", "application/zip",
            "rar", "application/vnd.rar");

    /**
     * extension → magic bytes family. DOCX/XLSX/ZIP share the PK signature (ZIP
     * container); the extension is the discriminator within the family.
     */
    private static final Map<Set<String>, byte[]> SIGNATURE_FAMILIES = Map.of(
            Set.of("doc"), new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}, // OLE
            Set.of("docx", "xlsx", "zip"), new byte[]{0x50, 0x4B, 0x03, 0x04}, // ZIP
            Set.of("pdf"), new byte[]{0x25, 0x50, 0x44, 0x46}, // %PDF-
            Set.of("rar"), new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07});          // Rar!
    private static final Set<String> TEXT_FAMILY = Set.of("csv");

    private final CardRepository cardRepository;
    private final CardAttachmentRepository attachmentRepository;
    private final Path basePath;

    public CardAttachmentService(
            CardRepository cardRepository,
            CardAttachmentRepository attachmentRepository,
            @Value("${ragkb.media.attachments-path}") String path) {
        this.cardRepository = cardRepository;
        this.attachmentRepository = attachmentRepository;
        // Canonical form ONCE here: absolute + normalized (mesma correção
        // documentada no BusinessRuleAttachmentService).
        this.basePath = Paths.get(path).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureBaseDir() throws IOException {
        Files.createDirectories(basePath);
    }

    @Transactional
    public CardAttachment upload(String cardId, MultipartFile file) {
        ensureCardExists(cardId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio ou ausente.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Anexo excede o limite de 20 MB.");
        }

        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new IllegalArgumentException("Nome de arquivo inválido.");
        }
        String extension = extensionOf(original);
        if (!ALLOWED_TYPES.containsKey(extension)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Aceitos: " + ALLOWED_TYPES.keySet());
        }

        assertMagicBytes(file, extension);

        String storedName = java.util.UUID.randomUUID().toString() + "." + extension;
        Path target = basePath.resolve(storedName).normalize();
        if (!target.startsWith(basePath)) {
            throw new IllegalStateException("Caminho de armazenamento inválido.");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar o anexo: " + e.getMessage(), e);
        }

        CardAttachment attachment = new CardAttachment(
                cardId, original, ALLOWED_TYPES.get(extension), file.getSize(),
                basePath.relativize(target).toString());
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void delete(String cardId, String attachmentId) {
        attachmentRepository.findByCardIdAndId(cardId, attachmentId).ifPresent(a -> {
            try {
                Files.deleteIfExists(basePath.resolve(a.getStoragePath()));
            } catch (IOException ignored) {
                // registro órfão no storage não bloqueia a exclusão
            }
            attachmentRepository.delete(a);
        });
    }

    /**
     * Remove os ARQUIVOS de todos os anexos do card. Chamado pelo
     * CardService.delete antes de excluir o card — as linhas somem por ON
     * DELETE CASCADE, mas o disco não tem cascade.
     */
    @Transactional
    public void deleteAllFilesForCard(String cardId) {
        for (CardAttachment a : attachmentRepository.findByCardIdOrderByUploadedAtAsc(cardId)) {
            try {
                Files.deleteIfExists(basePath.resolve(a.getStoragePath()));
            } catch (IOException ignored) {
                // mesmo critério do delete unitário
            }
            attachmentRepository.delete(a);
        }
    }

    public Path resolveStorage(CardAttachment attachment) {
        Path file = basePath.resolve(attachment.getStoragePath()).normalize();
        if (!file.startsWith(basePath) || !Files.isRegularFile(file)) {
            throw new CardNotFoundException(attachment.getId());
        }
        return file;
    }

    private void ensureCardExists(String cardId) {
        cardRepository.findById(cardId).orElseThrow(() -> new CardNotFoundException(cardId));
    }

    private void assertMagicBytes(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(16);
            if (TEXT_FAMILY.contains(extension)) {
                if (head.length == 0 || new String(head).contains("\u0000")) {
                    throw new IllegalArgumentException("Conteúdo do arquivo não corresponde a " + extension.toUpperCase());
                }
                return;
            }
            for (Map.Entry<Set<String>, byte[]> e : SIGNATURE_FAMILIES.entrySet()) {
                if (e.getKey().contains(extension)) {
                    byte[] sig = e.getValue();
                    if (head.length < sig.length || !startsWith(head, sig)) {
                        throw new IllegalArgumentException("Conteúdo do arquivo não corresponde a " + extension.toUpperCase());
                    }
                    return;
                }
            }
            throw new IllegalArgumentException("Tipo de arquivo não suportado: " + extension);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo.", e);
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
