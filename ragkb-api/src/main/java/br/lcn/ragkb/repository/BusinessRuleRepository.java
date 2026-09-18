package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.BusinessRuleStatus;
import br.lcn.ragkb.entity.EmbeddingStatus;

public interface BusinessRuleRepository extends JpaRepository<BusinessRule, String> {

    // ── Gestão (ADMIN/EDITOR) ─────────────────────────────────
    List<BusinessRule> findByStatusNotOrAuthorUsername(BusinessRuleStatus status, String authorUsername);

    boolean existsByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndIdNot(String title, String id);

    // ── Portal: leitura por setor ─────────────────────────────
    List<BusinessRule> findByStatusOrderByPublishedAtDesc(BusinessRuleStatus status);

    List<BusinessRule> findByStatusAndSectorNamesContainingOrderByPublishedAtDesc(
            BusinessRuleStatus status, String sectorName);

    // ── Guarda de exclusão de setor ───────────────────────────
    long countBySectorNamesContaining(String sectorName);

    // ── Retry job ─────────────────────────────────────────────
    List<BusinessRule> findByStatusAndEmbeddingStatusInOrderById(
            BusinessRuleStatus status, List<EmbeddingStatus> statuses);

    // ── Busca semântica — coluna embedding (vector) NÃO mapeada no
    //    Hibernate (ddl-auto: validate), por isso native query. ──────
    @Query(value = """
            SELECT r.id FROM business_rules r
            WHERE r.status = 'PUBLISHED'
              AND r.embedding IS NOT NULL
              AND (CAST(:sector AS text) IS NULL OR EXISTS (
                    SELECT 1 FROM business_rule_sectors s
                    WHERE s.rule_id = r.id AND s.sector_name = CAST(:sector AS text)))
            ORDER BY r.embedding <=> CAST(:qv AS vector)
            LIMIT 20
            """, nativeQuery = true)
    List<String> semanticSearchIds(@Param("sector") String sector, @Param("qv") String queryVector);

    @Modifying
    @Query(value = """
            UPDATE business_rules
            SET embedding = CAST(:qv AS vector), embedding_status = 'DONE'
            WHERE id = :id
            """, nativeQuery = true)
    void saveEmbedding(@Param("id") String id, @Param("qv") String vectorLiteral);

    @Modifying
    @Query("update BusinessRule r set r.embeddingStatus = :status where r.id = :id")
    void updateEmbeddingStatus(@Param("id") String id, @Param("status") EmbeddingStatus status);
}