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

import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.BusinessRuleAttachment;
import br.lcn.ragkb.exception.BusinessRuleNotFoundException;
import br.lcn.ragkb.repository.BusinessRuleAttachmentRepository;
import br.lcn.ragkb.repository.BusinessRuleRepository;
import jakarta.annotation.PostConstruct;

@Service
public class BusinessRuleAttachmentService {

    private static final long MAX_SIZE_BYTES = 20 * 1024 * 1024;

    /** extension → content type emitted on download (whitelist-driven). */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "csv", "text/csv",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "zip", "application/zip",
            "rar", "application/vnd.rar");

    /** extension → magic bytes family. DOCX/XLSX/ZIP share the PK signature
     * (ZIP container); the extension is the discriminator within the family. */
    private static final Map<Set<String>, byte[]> SIGNATURE_FAMILIES = Map.of(
            Set.of("doc"), new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0},   // OLE
            Set.of("docx", "xlsx", "zip"), new byte[]{0x50, 0x4B, 0x03, 0x04},       // ZIP
            Set.of("pdf"), new byte[]{0x25, 0x50, 0x44, 0x46},                       // %PDF-
            Set.of("rar"), new byte[]{0x52, 0x61, 0x72, 0x21, 0x1A, 0x07});          // Rar!
    private static final Set<String> TEXT_FAMILY = Set.of("csv");

    private final BusinessRuleRepository ruleRepository;
    private final BusinessRuleAttachmentRepository attachmentRepository;
    private final Path basePath;

    public BusinessRuleAttachmentService(
            BusinessRuleRepository ruleRepository,
            BusinessRuleAttachmentRepository attachmentRepository,
            @Value("${ragkb.media.attachments-path}") String path) {
        this.ruleRepository = ruleRepository;
        this.attachmentRepository = attachmentRepository;
        // Canonical form ONCE here: absolute + normalized. Every startsWith()
        // guard compares against this same form — comparing a normalized
        // target against a relative "./data/attachments" base always fails
        // (element "data" vs "."), which broke 100% of uploads.
        this.basePath = Paths.get(path).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureBaseDir() throws IOException {
        Files.createDirectories(basePath);
    }

    @Transactional
    public BusinessRuleAttachment upload(String ruleId, MultipartFile file, String username) {
        BusinessRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessRuleNotFoundException(ruleId));

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

        BusinessRuleAttachment attachment = new BusinessRuleAttachment(
                rule, original, ALLOWED_TYPES.get(extension), file.getSize(),
                basePath.relativize(target).toString(), username);
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void delete(String ruleId, String attachmentId) {
        attachmentRepository.findByRuleIdAndId(ruleId, attachmentId).ifPresent(a -> {
            try {
                Files.deleteIfExists(basePath.resolve(a.getStoragePath()));
            } catch (IOException ignored) {
                // registro órfão no storage não bloqueia a exclusão lógica
            }
            attachmentRepository.delete(a);
        });
    }

    public Path resolveStorage(BusinessRuleAttachment attachment) {
        Path file = basePath.resolve(attachment.getStoragePath()).normalize();
        if (!file.startsWith(basePath) || !Files.isRegularFile(file)) {
            throw new BusinessRuleNotFoundException(attachment.getId());
        }
        return file;
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