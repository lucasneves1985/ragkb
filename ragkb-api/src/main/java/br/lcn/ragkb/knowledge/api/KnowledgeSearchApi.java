package br.lcn.ragkb.knowledge.api;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;

public interface KnowledgeSearchApi {
    List<Document> hybridSearch(String question, String filterExpr,
            boolean isAdmin, List<String> roles, String userSector,
            double similarityThreshold, int topK, int lexicalTopK, int rrfK);

    Map<String, String> findFilenames(List<String> documentIds);
}
