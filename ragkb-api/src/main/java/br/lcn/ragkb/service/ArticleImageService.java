package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.ArticleImageUploadResponse;
import br.lcn.ragkb.exception.InvalidImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ArticleImageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    // Whitelist por magic bytes — o Content-Type do multipart NÃO é confiável
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46} // RIFF (valida WEBP nos bytes 8-11)
    );

    private final Path basePath;

    public ArticleImageService(@Value("${ragkb.media.path}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    public ArticleImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Arquivo vazio");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidImageException("Imagem excede o limite de 5 MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(basePath);
            Path target = basePath.resolve(filename).normalize();
            // Defesa em profundidade: o resolve acima nunca sai do basePath,
            // pois o filename é gerado aqui — a validação explícita cobre
            // refatorações futuras que aceitem nome externo
            if (!target.startsWith(basePath)) {
                throw new InvalidImageException("Caminho inválido");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao salvar imagem", e);
        }

        return new ArticleImageUploadResponse("/api/media/articles/" + filename);
    }

    private String detectExtension(MultipartFile file) {
        byte[] header = readHeader(file);
        for (Map.Entry<String, byte[]> entry : SIGNATURES.entrySet()) {
            if (startsWith(header, entry.getValue())) {
                // WebP: RIFF + "WEBP" nos bytes 8-11
                if ("webp".equals(entry.getKey())
                        && (header.length < 12 || header[8] != 'W' || header[9] != 'E'
                        || header[10] != 'B' || header[11] != 'P')) {
                    continue;
                }
                return entry.getKey();
            }
        }
        throw new InvalidImageException("Formato não suportado. Use JPG, PNG ou WebP.");
    }

    private byte[] readHeader(MultipartFile file) {
        try (var in = file.getInputStream()) {
            return in.readNBytes(12);
        } catch (IOException e) {
            throw new InvalidImageException("Falha na leitura do arquivo");
        }
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) return false;
        }
        return true;
    }
}