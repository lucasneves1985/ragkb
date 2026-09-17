package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Article;
import br.lcn.ragkb.entity.ArticleStatus;

public interface ArticleRepository extends JpaRepository<Article, String> {

    List<Article> findByStatusNotOrAuthorUsername(ArticleStatus status, String authorUsername);

    // ── Portal: leitura pública por setor ─────────────────────

    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);

    List<Article> findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(ArticleStatus status, String title);

    List<Article> findByStatusAndAllowedSectorsContainingOrderByPublishedAtDesc(ArticleStatus status, String sector);

    List<Article> findByStatusAndAllowedSectorsContainingAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            ArticleStatus status, String sector, String title);
}