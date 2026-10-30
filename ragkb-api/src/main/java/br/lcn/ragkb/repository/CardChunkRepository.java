package br.lcn.ragkb.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.lcn.ragkb.entity.CardChunk;
import br.lcn.ragkb.entity.EmbeddingStatus;

public interface CardChunkRepository extends JpaRepository<CardChunk, String> {

    List<CardChunk> findByCardId(String cardId);

    // ── Retry job ─────────────────────────────────────────────
    List<CardChunk> findByEmbeddingStatusInOrderById(Collection<EmbeddingStatus> statuses);

    // ── Re-embedding: reconstrução limpa por card ─────────────
    @Modifying
    @Query(value = "DELETE FROM card_chunks WHERE card_id = :id", nativeQuery = true)
    void deleteByCardId(@Param("id") String id);

    @Modifying
    @Query(value = """
            UPDATE card_chunks
            SET embedding = CAST(:qv AS vector), embedding_status = 'DONE'
            WHERE id = :id
            """, nativeQuery = true)
    void saveEmbedding(@Param("id") String id, @Param("qv") String vectorLiteral);

    @Modifying
    @Query("update CardChunk c set c.embeddingStatus = :status where c.id = :id")
    void updateEmbeddingStatus(@Param("id") String id, @Param("status") EmbeddingStatus status);

    // ── Busca semântica híbrida retornando SCORES (não apenas ids).
    //    Match lexical (ILIKE) e match vetorial coexistem; o corte de
    //    relevância é RELATIVO ao melhor score e acontece no service
    //    (CardQueryService) — threshold absoluto fixo não funciona para
    //    queries curtas (falso positivo) E perguntas longas (recall baixo)
    //    ao mesmo tempo. NÃO filtra por status — cards encerrados entram
    //    (decisão 3.3). ──
    @Query(value = """
            SELECT ch.id AS id,
                   ch.content ILIKE '%' || CAST(:query AS text) || '%' AS lexical,
                   CASE WHEN ch.embedding IS NOT NULL
                        THEN 1 - (ch.embedding <=> CAST(:qv AS vector))
                        ELSE 0 END AS similarity
            FROM card_chunks ch
            WHERE (CAST(:projectId AS text) IS NULL OR ch.project_id = CAST(:projectId AS text))
              AND (
                    ch.content ILIKE '%' || CAST(:query AS text) || '%'
                    OR (ch.embedding IS NOT NULL
                        AND 1 - (ch.embedding <=> CAST(:qv AS vector)) >= :similarityThreshold)
                  )
            ORDER BY lexical DESC, similarity DESC
            LIMIT 40
            """, nativeQuery = true)
    List<CardChunkScore> semanticSearchCandidates(@Param("projectId") String projectId,
            @Param("qv") String queryVector,
            @Param("query") String query,
            @Param("similarityThreshold") double similarityThreshold);
}
