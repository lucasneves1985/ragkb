package br.lcn.ragkb.repository;

import br.lcn.ragkb.entity.Article;
import br.lcn.ragkb.entity.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, String> {

    List<Article> findByStatusNotOrAuthorUsername(ArticleStatus status, String authorUsername);
}