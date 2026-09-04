package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.IngestRequest;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.exception.DuplicateDocumentException;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngestionService {

    private static final int CHUNK_SIZE = 512;
    private static final int MIN_CHUNK_SIZE_CHARS = 64;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 10000;

    private final VectorStore vectorStore;
    private final DocumentMetadataRepository metadataRepository;
    private final DocumentLifecycleService lifecycleService;

    @Transactional
    public DocumentMetadata ingest(MultipartFile file, IngestRequest request) {
        byte[] bytes = readBytes(file);
        String contentHash = sha256(bytes);

        // Idempotência: mesmo conteúdo não é re-embeddado
        if (metadataRepository.existsByContentHash(contentHash)) {
            throw new DuplicateDocumentException("Documento já ingerido com o hash: " + contentHash);
        }

        // Supersessão ANTES de inserir o novo: se o novo falhar, a transação
        // inteira é revertida e o antigo continua ACTIVE
        if (request.supersedesDocumentId() != null) {
            lifecycleService.supersede(request.supersedesDocumentId());
        }

        // Parse (Tika) -> texto-fonte completo + chunks
        List<Document> parsed = new TikaDocumentReader(new ByteArrayResource(bytes)).get();
        String sourceText = parsed.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(parsed);

        String documentId = UUID.randomUUID().toString();
        chunks.forEach(chunk -> chunk.getMetadata().putAll(Map.of(
                "documentId", documentId,
                "filename", file.getOriginalFilename(),
                "sector", request.sector(),
                "allowedRoles", request.allowedRoles(),
                "contentHash", contentHash
        )));

        // Embedding via Gemini + persistência no pgvector
        vectorStore.add(chunks);

        return metadataRepository.save(new DocumentMetadata(
                documentId, file.getOriginalFilename(), request.sector(),
                contentHash, chunks.size(), request.supersedesDocumentId(),
                sourceText, request.allowedRoles()));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Falha na leitura dos bytes do arquivo", e);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", e);
        }
    }
}