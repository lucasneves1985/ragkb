package br.lcn.ragkb.project;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectShareRepository extends JpaRepository<ProjectShare, String> {

    boolean existsByProjectIdAndUserId(String projectId, Long userId);

    List<ProjectShare> findByProjectIdOrderBySharedAtAsc(String projectId);

    Optional<ProjectShare> findByProjectIdAndUserId(String projectId, Long userId);

    /**
     * Delete derivado — exige transação ativa no chamador (os métodos do
     * ProjectService são @Transactional).
     */
    void deleteByProjectIdAndUserId(String projectId, Long userId);
}