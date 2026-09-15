package br.lcn.ragkb.controller;

import br.lcn.ragkb.exception.ImageNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/media/articles")
public class MediaController {

    // Apenas UUID v4 + extensão conhecida — qualquer outra coisa é 404,
    // o que mata path traversal por construção (../, %2e%2e, etc.)
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG,
            "webp", MediaType.valueOf("image/webp")
    );

    private final Path basePath;

    public MediaController(@Value("${ragkb.media.path}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    @GetMapping("/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FileSystemResource> serve(@PathVariable String filename) {
        if (!FILENAME_PATTERN.matcher(filename).matches()) {
            throw new ImageNotFoundException(filename);
        }

        Path file = basePath.resolve(filename).normalize();
        if (!file.startsWith(basePath) || !Files.isRegularFile(file)) {
            throw new ImageNotFoundException(filename);
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        return ResponseEntity.ok()
                .contentType(MEDIA_TYPES.get(extension))
                .body(new FileSystemResource(file));
    }
}