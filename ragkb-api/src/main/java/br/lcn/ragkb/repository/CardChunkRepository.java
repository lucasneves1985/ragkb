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

    // ── Busca semântica híbrida (vetorial + ILIKE), mesmo padrão
    //    do BusinessRuleRepository.semanticSearchIds. Retorna chunk ids
    //    ordenados por relevância; o join com cards acontece no service.
    //    NÃO filtra por status — cards encerrados entram (decisão 3.3). ──
    @Query(value = """
            SELECT ch.id FROM card_chunks ch
            WHERE (CAST(:projectId AS text) IS NULL OR ch.project_id = CAST(:projectId AS text))
              AND (
                    (ch.embedding IS NOT NULL
                     AND 1 - (ch.embedding <=> CAST(:qv AS vector)) >= :similarityThreshold)
                    OR ch.content ILIKE '%' || CAST(:query AS text) || '%'
                  )
            ORDER BY CASE
                        WHEN ch.content ILIKE '%' || CAST(:query AS text) || '%'
                        THEN 0 ELSE 1
                     END,
                     ch.embedding <=> CAST(:qv AS vector)
            LIMIT 20
            """, nativeQuery = true)
    List<String> semanticSearchChunkIds(@Param("projectId") String projectId,
            @Param("qv") String queryVector,
            @Param("query") String query,
            @Param("similarityThreshold") double similarityThreshold);
}
