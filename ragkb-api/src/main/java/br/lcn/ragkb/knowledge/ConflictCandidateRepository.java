package br.lcn.ragkb.knowledge;



import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConflictCandidateRepository extends JpaRepository<ConflictCandidate, Long> {

    List<ConflictCandidate> findAllByStatusOrderByScoreDesc(ConflictStatus status);

    // Dedupe do scan: ignora a tentativa de re-inserir candidatos para os mesmos chunks
    @Query("""
            SELECT COUNT(c) > 0 FROM ConflictCandidate c
            WHERE (c.documentIdA = :docA AND c.documentIdB = :docB AND c.chunkIdA = :chunkA AND c.chunkIdB = :chunkB)
               OR (c.documentIdA = :docB AND c.documentIdB = :docA AND c.chunkIdA = :chunkB AND c.chunkIdB = :chunkA)
            """)
    boolean existsConflict(@Param("docA") String docA,
                           @Param("docB") String docB,
                           @Param("chunkA") String chunkA,
                           @Param("chunkB") String chunkB);

    // Auto-resolução: quando um documento deixa de ser ACTIVE (supersede/archive),
    // todos os candidatos OPEN que o envolvem são resolvidos na mesma transação.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ConflictCandidate c
            SET c.status = :status, c.note = :note,
                c.updatedAt = :now, c.updatedBy = :updatedBy
            WHERE c.status = br.lcn.ragkb.knowledge.ConflictStatus.OPEN
              AND (c.documentIdA = :docId OR c.documentIdB = :docId)
            """)
    int resolveOpenCandidatesForDocument(@Param("docId") String docId,
                                         @Param("status") ConflictStatus status,
                                         @Param("note") String note,
                                         @Param("now") Instant now,
                                         @Param("updatedBy") String updatedBy);
}
