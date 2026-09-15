package br.lcn.ragkb.service;

import br.lcn.ragkb.entity.AppRole;
import br.lcn.ragkb.entity.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleIngestionService {

    private static final int CHUNK_SIZE = 512;
    private static final int MIN_CHUNK_SIZE_CHARS = 64;
    private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
    private static final int MAX_NUM_CHUNKS = 10000;

    private final VectorStore vectorStore;
    private final ArticleSanitizationService sanitizationService;

    /**
     * Ingests a published article into the vector store.
     * Replaces any previous chunks (delete by articleId) — safe to call
     * on publish and on every re-ingestion after update.
     */
    public int ingest(Article article, String articleUrl) {
        vectorStore.delete("articleId == '" + article.getId() + "'");

        String text = sanitizationService.extractText(article.getContent());
        if (text.isBlank()) {
            throw new IllegalStateException("Artigo sem texto extraível: " + article.getId());
        }

        Document source = new Document(text);
        List<Document> chunks = splitter().apply(List.of(source));

        // Sobrecarga semântica documentada: allowedRoles = SEM restrição de
        // papel. O gate real é allowedSectors. Ver AppRole.allAuthorities().
        chunks.forEach(chunk -> chunk.getMetadata().putAll(Map.of(
                "sourceType", "ARTICLE",
                "articleId", article.getId(),
                "articleTitle", article.getTitle(),
                "articleUrl", articleUrl,
                "allowedSectors", article.getAllowedSectors(),
                "allowedRoles", AppRole.allAuthorities()
        )));

        vectorStore.add(chunks);
        return chunks.size();
    }

    public void deleteVectors(String articleId) {
        vectorStore.delete("articleId == '" + articleId + "'");
    }

    private TokenTextSplitter splitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(MAX_NUM_CHUNKS)
                .withKeepSeparator(true)
                .build();
    }
}