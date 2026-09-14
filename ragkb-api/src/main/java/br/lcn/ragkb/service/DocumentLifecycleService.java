package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.DocumentDto;
import br.lcn.ragkb.entity.ConflictStatus;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.DocumentStatus;
import br.lcn.ragkb.exception.DocumentNotFoundException;
import br.lcn.ragkb.repository.ConflictCandidateRepository;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentLifecycleService {

    private static final int CHUNK_SIZE = 512;
    private static final int MIN_CHUNK_SIZE_CHARS = 64;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 10000;

    private final VectorStore vectorStore;
    private final DocumentMetadataRepository metadataRepository;
    private final ConflictCandidateRepository conflictRepository;

    @Transactional
    public DocumentDto archive(String documentId, String username) {
        DocumentMetadata doc = findActive(documentId);
        vectorStore.delete("documentId == '" + documentId + "'");
        doc.archive();
        DocumentMetadata saved = metadataRepository.save(doc);
        resolveConflictsFor(documentId, username);
        return DocumentDto.from(saved);
    }

    @Transactional
    public DocumentDto reactivate(String documentId, List<String> allowedRoles, String username) {
        DocumentMetadata doc = metadataRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (doc.getStatus() != DocumentStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Somente documentos ARCHIVED podem ser reativados. Status atual: " + doc.getStatus());
        }
        if (doc.getSourceText() == null || doc.getSourceText().isBlank()) {
            throw new IllegalStateException(
                    "Documento sem texto-fonte armazenado. Reenvie o arquivo para reativar.");
        }

        List<String> roles = (allowedRoles != null && !allowedRoles.isEmpty())
                ? allowedRoles
                : doc.getAllowedRoles();
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException(
                    "Documento sem papéis de acesso. Informe allowedRoles na reativação.");
        }

        List<Document> chunks = split(doc.getSourceText());
        List<String> sectors = doc.getAllowedSectors() != null ? doc.getAllowedSectors() : List.of();
        chunks.forEach(chunk -> chunk.getMetadata().putAll(Map.of(
                "documentId", doc.getId(),
                "sector", doc.getSector(),
                "allowedRoles", roles,
                "allowedSectors", sectors,
                "contentHash", doc.getContentHash()
        )));

        vectorStore.add(chunks);

        doc.setStatus(DocumentStatus.ACTIVE);
        doc.setAllowedRoles(roles);
        doc.setChunkCount(chunks.size());
        return DocumentDto.from(metadataRepository.save(doc));
    }

    @Transactional
    public void supersede(String documentId, String username) {
        DocumentMetadata old = findActive(documentId);
        vectorStore.delete("documentId == '" + documentId + "'");
        old.supersede();
        metadataRepository.save(old);
        resolveConflictsFor(documentId, username);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listAll() {
        return metadataRepository.findAll().stream()
                .map(DocumentDto::from)
                .toList();
    }

    private void resolveConflictsFor(String documentId, String username) {
        conflictRepository.resolveOpenCandidatesForDocument(
                documentId,
                ConflictStatus.RESOLVED,
                "Auto-resolvido: documento não está mais ACTIVE",
                Instant.now(),
                username);
    }

    private DocumentMetadata findActive(String documentId) {
        DocumentMetadata doc = metadataRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (doc.getStatus() != DocumentStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Documento " + documentId + " não está ACTIVE — status: " + doc.getStatus());
        }
        return doc;
    }

    private List<Document> split(String text) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .withKeepSeparator(true)
                .build();
        return splitter.apply(List.of(new Document(text)));
    }
}