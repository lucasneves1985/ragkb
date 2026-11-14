package br.lcn.ragkb.knowledge;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, String> {

    List<Article> findByStatusNotOrAuthorUsername(ArticleStatus status, String authorUsername);

    // ── Portal: leitura pública por setor ─────────────────────

    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);

    List<Article> findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(ArticleStatus status, String title);

    List<Article> findByStatusAndAllowedSectorsContainingOrderByPublishedAtDesc(ArticleStatus status, String sector);

    List<Article> findByStatusAndAllowedSectorsContainingAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            ArticleStatus status, String sector, String title);
}