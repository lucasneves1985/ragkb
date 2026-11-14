package br.lcn.ragkb.knowledge.search;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.lcn.ragkb.knowledge.DocumentMetadata;
import br.lcn.ragkb.knowledge.DocumentMetadataRepository;
import br.lcn.ragkb.knowledge.api.KnowledgeSearchApi;
import br.lcn.ragkb.knowledge.search.KbLexicalSearchRepository.LexicalHit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSearchService implements KnowledgeSearchApi {

    private static final ObjectMapper METADATA_MAPPER = new ObjectMapper();

    private final VectorStore vectorStore;
    private final KbLexicalSearchRepository kbLexicalSearchRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    /**
     * Executa os DOIS caminhos de busca e funde por Reciprocal Rank Fusion:
     * score = Σ 1/(RRF_K + posição). Documento presente nas duas listas é
     * promovido (consenso semântico+lexical vence match em lista única) e as
     * escalas incomensuráveis nunca são somadas diretamente. Lista vazia nos
     * dois caminhos → vazio (fallback follow-up/ticket segue no fluxo do ask).
     */
    @Override
    public List<Document> hybridSearch(String question, String filterExpr,
            boolean isAdmin, List<String> roles, String userSector,
            double similarityThreshold, int topK, int lexicalTopK, int rrfK) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold);
        if (filterExpr != null) {
            builder.filterExpression(filterExpr);
        }
        List<Document> semanticHits = vectorStore.similaritySearch(builder.build());

        List<LexicalHit> lexicalHits;
        try {
            lexicalHits = kbLexicalSearchRepository.search(
                    question, roles, userSector, isAdmin, lexicalTopK);
        } catch (Exception e) {
            // Falha lexical não derruba a consulta — degrada para semântica pura
            // (mesmo contrato de degradação dos embeddings dos cards).
            log.warn("Busca lexical da KB falhou — usando apenas semântica: {}", e.getMessage());
            lexicalHits = List.of();
        }

        log.info("Busca híbrida KB: {} hit(s) semântico(s), {} hit(s) lexical(is) — fundindo",
                semanticHits.size(), lexicalHits.size());
        return fuseByReciprocalRank(semanticHits, lexicalHits, topK, rrfK);
    }

    @Override
    public Map<String, String> findFilenames(List<String> documentIds) {
        return documentMetadataRepository.findAllById(documentIds).stream()
                .collect(java.util.stream.Collectors.toMap(DocumentMetadata::getId, DocumentMetadata::getFilename));
    }

    /**
     * Fusão por posição: rank começa em 1. Empates de score mantêm a ordem de
     * inserção (sort estável) — semântica antes, lexical depois. Quando só uma
     * lista tem conteúdo, ela é preservada na ordem original (RRF degenera para
     * a própria lista).
     */
    private List<Document> fuseByReciprocalRank(List<Document> semanticHits,
            List<LexicalHit> lexicalHits, int topK, int rrfK) {
        if (semanticHits.isEmpty() && lexicalHits.isEmpty()) {
            return List.of();
        }
        if (lexicalHits.isEmpty()) {
            return semanticHits;
        }
        if (semanticHits.isEmpty()) {
            return lexicalHits.stream().map(this::toDocument).toList();
        }

        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Document> docsById = new LinkedHashMap<>();
        for (int i = 0; i < semanticHits.size(); i++) {
            String id = semanticHits.get(i).getId();
            rrfScores.merge(id, 1.0 / (rrfK + i + 1), Double::sum);
            docsById.put(id, semanticHits.get(i));
        }
        for (int i = 0; i < lexicalHits.size(); i++) {
            LexicalHit hit = lexicalHits.get(i);
            rrfScores.merge(hit.id(), 1.0 / (rrfK + i + 1), Double::sum);
            if (!docsById.containsKey(hit.id())) {
                docsById.put(hit.id(), toDocument(hit));
            }
        }
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> docsById.get(entry.getKey()))
                .toList();
    }

    /**
     * Converte um hit lexical em Document com o mesmo formato de metadata que o
     * caminho semântico hidrata (valores como documentId/filename/articleId são
     * comparados como string no fluxo de fontes — listas como allowedRoles
     * voltam como List, igual ao hydration do Spring AI).
     */
    private Document toDocument(LexicalHit hit) {
        Map<String, Object> metadata;
        try {
            metadata = METADATA_MAPPER.readValue(hit.metadata(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (JsonProcessingException e) {
            log.warn("Metadata JSON inválido no chunk {} — usando metadata vazio", hit.id());
            metadata = Map.of();
        }
        return Document.builder()
                .id(hit.id())
                .text(hit.content())
                .metadata(metadata)
                .build();
    }
}
