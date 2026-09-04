package br.lcn.ragkb.service;

import br.lcn.ragkb.entity.ConflictCandidate;
import br.lcn.ragkb.repository.ConflictCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    // Mais alto que o threshold de resposta (0.65): aqui queremos "mesmo conteúdo",
    // não "tópico relacionado". Calibrar com dados reais.
    private static final double CONFLICT_THRESHOLD = 0.85;
    private static final int TOP_K = 5;
    private static final int SNIPPET_LENGTH = 200;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ConflictCandidateRepository conflictRepository;

    public List<ConflictCandidate> scan() {
        List<ChunkRecord> chunks = loadActiveChunks();
        log.info("Detecção de conflito: {} chunks ativos", chunks.size());

        Set<String> seenPairs = new HashSet<>();
        List<ConflictCandidate> created = new ArrayList<>();

        for (ChunkRecord chunk : chunks) {
            List<Document> hits = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(chunk.content())
                            .topK(TOP_K)
                            .build());

            for (Document hit : hits) {
                String hitDocId = String.valueOf(hit.getMetadata().get("documentId"));
                if (hitDocId.equals(chunk.documentId())) {
                    continue; // o próprio chunk
                }
                double score = hit.getScore();
                if (score < CONFLICT_THRESHOLD) {
                    continue;
                }
                String pairKey = chunk.documentId().compareTo(hitDocId) <= 0
                        ? chunk.documentId() + "|" + hitDocId
                        : hitDocId + "|" + chunk.documentId();
                if (!seenPairs.add(pairKey)) {
                    continue; // par já processado nesta execução
                }
                if (conflictRepository.existsConflict(
                        chunk.documentId(), hitDocId, chunk.chunkId(), hit.getId())) {
                    continue; // já existe candidato cadastrado
                }
                ConflictCandidate candidate = new ConflictCandidate(
                        chunk.documentId(), hitDocId, chunk.chunkId(), hit.getId(),
                        score, truncate(chunk.content()), truncate(hit.getText()));
                created.add(conflictRepository.save(candidate));
            }
        }
        log.info("Detecção de conflito: {} novos candidatos", created.size());
        return created;
    }

    private List<ChunkRecord> loadActiveChunks() {
        String sql = """
                SELECT vs.id, vs.content, vs.metadata
                FROM vector_store vs
                JOIN documents d ON d.id = vs.metadata->>'documentId'
                WHERE d.status = 'ACTIVE'
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> meta = parseMetadata(rs.getObject("metadata"));
            return new ChunkRecord(
                    String.valueOf(meta.get("documentId")),
                    rs.getString("id"),
                    rs.getString("content"));
        });
    }

    private Map<String, Object> parseMetadata(Object raw) {
        try {
            // String.valueOf cobre String e PGobject (toString() == getValue()).
            // Evita import de org.postgresql.util.PGobject, que não está no
            // classpath de compilação (driver em escopo runtime).
            String json = String.valueOf(raw);
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= SNIPPET_LENGTH ? text : text.substring(0, SNIPPET_LENGTH) + "...";
    }

    private record ChunkRecord(String documentId, String chunkId, String content) {}
}