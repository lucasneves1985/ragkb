package br.lcn.ragkb.service;

import br.lcn.ragkb.exception.InvalidImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AvatarImageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Pattern AVATAR_URL = Pattern.compile("^/api/media/avatars/([0-9a-f-]{36}\\.(jpg|png|webp))$");
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46});

    private final Path basePath;

    public AvatarImageService(@Value("${ragkb.media.avatars-path}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidImageException("Selecione uma foto.");
        if (file.getSize() > MAX_SIZE_BYTES) throw new InvalidImageException("A foto deve ter no máximo 5 MB.");

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(basePath);
            Path target = basePath.resolve(filename).normalize();
            if (!target.startsWith(basePath)) throw new InvalidImageException("Caminho de arquivo inválido.");
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar a foto.", e);
        }
        return "/api/media/avatars/" + filename;
    }

    public void delete(String avatarUrl) {
        Matcher matcher = AVATAR_URL.matcher(avatarUrl);
        if (!matcher.matches()) return;
        Path target = basePath.resolve(matcher.group(1)).normalize();
        if (!target.startsWith(basePath)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível remover a foto anterior.", e);
        }
    }

    private String detectExtension(MultipartFile file) {
        byte[] header;
        try (var input = file.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException e) {
            throw new InvalidImageException("Não foi possível ler a foto.");
        }
        for (var entry : SIGNATURES.entrySet()) {
            byte[] signature = entry.getValue();
            boolean matches = header.length >= signature.length;
            for (int i = 0; matches && i < signature.length; i++) matches = header[i] == signature[i];
            if (matches && "webp".equals(entry.getKey())) {
                matches = header.length >= 12 && header[8] == 'W' && header[9] == 'E'
                        && header[10] == 'B' && header[11] == 'P';
            }
            if (matches) return entry.getKey();
        }
        throw new InvalidImageException("Formato inválido. Use JPG, PNG ou WebP.");
    }
}
