package br.lcn.ragkb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.lcn.ragkb.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    /**
     * Visibilidade (V20): owner + participantes compartilhados. Projetos
     * legados (created_by NULL) permanecem visíveis a todos — decisão do autor:
     * sem dados de produção, sem backfill.
     */
    @Query("""
            SELECT p FROM Project p
            WHERE p.createdBy IS NULL
               OR p.createdBy = :userId
               OR EXISTS (SELECT s FROM ProjectShare s
                          WHERE s.projectId = p.id AND s.userId = :userId)
            ORDER BY p.name
            """)
    List<Project> findVisibleTo(@Param("userId") Long userId);

    /**
     * Projeto único com o mesmo predicado de visibilidade — 404 para quem não
     * participa (não revela a existência do projeto alheio, mesmo padrão do
     * getOwnedCard dos cards).
     */
    @Query("""
            SELECT p FROM Project p
            WHERE p.id = :id
              AND (p.createdBy IS NULL
                   OR p.createdBy = :userId
                   OR EXISTS (SELECT s FROM ProjectShare s
                              WHERE s.projectId = p.id AND s.userId = :userId))
            """)
    Optional<Project> findVisibleById(@Param("id") String id, @Param("userId") Long userId);

    /**
     * Participação efetiva: owner OU compartilhado. Projetos legados
     * (created_by NULL) NÃO tornam ninguém participante — a privacidade de
     * cards por criador permanece neles.
     */
    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM Project p
            WHERE p.id = :projectId
              AND (p.createdBy = :userId
                   OR EXISTS (SELECT s FROM ProjectShare s
                              WHERE s.projectId = p.id AND s.userId = :userId))
            """)
    boolean isParticipant(@Param("projectId") String projectId, @Param("userId") Long userId);

    /**
     * Reindexação GLOBAL das positions por status — executada no PRIMEIRO
     * compartilhamento de um projeto (V20): positions por criador podem colidir
     * (V18 permite) e o board compartilhado exige ordem única. Ordem
     * determinística: position atual, depois created_at.
     *
     * Vive aqui (e não no CardRepository) porque é uma operação de nível de
     * projeto disparada pelo compartilhamento — e o CardRepository tem bloco
     * legado pendente de limpeza que não deve ser tocado neste marco. Requer
     * transação ativa no chamador.
     */
    @Modifying
    @Query(value = """
            UPDATE cards AS c
            SET position = r.rn
            FROM (
                SELECT id,
                       ROW_NUMBER() OVER (PARTITION BY status ORDER BY position, created_at) - 1 AS rn
                FROM cards
                WHERE project_id = :projectId
            ) r
            WHERE c.id = r.id
            """, nativeQuery = true)
    void reindexPositionsGlobally(@Param("projectId") String projectId);
}
