package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.ArticleDto;
import br.lcn.ragkb.dto.CreateArticleRequest;
import br.lcn.ragkb.dto.UpdateArticleRequest;
import br.lcn.ragkb.entity.Article;
import br.lcn.ragkb.entity.ArticleStatus;
import br.lcn.ragkb.exception.ArticleNotFoundException;
import br.lcn.ragkb.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleLifecycleService {

    private final ArticleRepository articleRepository;
    private final ArticleSanitizationService sanitizationService;
    private final ArticleIngestionService ingestionService;
    private final UserService userService;

    @Value("${ragkb.articles.public-base-url}")
    private String baseUrl;

    @Transactional
    public ArticleDto create(CreateArticleRequest request, String username, boolean isAdmin) {
        validateSectorPermission(request.sector(), request.allowedSectors(), username, isAdmin);

        Article article = new Article(
                request.title(),
                sanitizationService.sanitize(request.content()),
                request.sector(),
                username,
                request.allowedSectors());

        return toDto(articleRepository.save(article));
    }

    @Transactional
    public ArticleDto update(String id, UpdateArticleRequest request, String username, boolean isAdmin) {
        Article article = findOwned(id, username, isAdmin);

        String sanitized = sanitizationService.sanitize(request.content());
        article.updateContent(request.title(), sanitized, request.allowedSectors());

        // Regra de re-ingestão: artigo publicado + versão mudou = vetores obsoletos.
        // Delete + re-embed com os novos allowedSectors (metadata vive no vetor).
        if (article.isPublished()) {
            int chunkCount = ingestionService.ingest(article, articleUrl(article));
            article.setChunkCount(chunkCount);
        }

        return toDto(articleRepository.save(article));
    }

    @Transactional
    public ArticleDto publish(String id, String username, boolean isAdmin) {
        Article article = findOwned(id, username, isAdmin);
        if (!article.isDraft()) {
            throw new IllegalStateException("Somente artigos em rascunho podem ser publicados");
        }

        article.publish();
        articleRepository.saveAndFlush(article);

        int chunkCount = ingestionService.ingest(article, articleUrl(article));
        article.setChunkCount(chunkCount);

        return toDto(articleRepository.save(article));
    }

    @Transactional
    public ArticleDto archive(String id, String username, boolean isAdmin) {
        Article article = findOwned(id, username, isAdmin);

        ingestionService.deleteVectors(article.getId());
        article.archive();

        return toDto(articleRepository.save(article));
    }

    /**
     * Visibility: ADMIN sees everything; EDITOR sees published articles
     * plus own drafts. DRAFTS are never ingested, so the agent can never
     * retrieve unpublished content — this listing is management-only.
     */
    @Transactional(readOnly = true)
    public List<ArticleDto> list(String username, boolean isAdmin) {
        List<Article> articles = isAdmin
                ? articleRepository.findAll()
                : articleRepository.findByStatusNotOrAuthorUsername(
                ArticleStatus.DRAFT, username);
        return articles.stream().map(this::toDto).toList();
    }

    private Article findOwned(String id, String username, boolean isAdmin) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException(id));
        if (!isAdmin && !article.getAuthorUsername().equals(username)) {
            throw new ArticleNotFoundException(id); // 404, não 403 — não revela existência
        }
        return article;
    }

    private void validateSectorPermission(String sector, List<String> allowedSectors,
                                          String username, boolean isAdmin) {
        if (allowedSectors == null || allowedSectors.isEmpty()) {
            throw new IllegalArgumentException("allowedSectors é obrigatório e não pode ser vazio");
        }
        if (!isAdmin) {
            String editorSector = userService.findByUsername(username);
            if (!sector.equals(editorSector)) {
                throw new IllegalArgumentException(
                        "Editor só pode criar artigos do seu setor: " + editorSector);
            }
            if (!allowedSectors.contains(editorSector)) {
                throw new IllegalArgumentException(
                        "allowedSectors deve incluir o setor do editor: " + editorSector);
            }
        }
    }

    private String articleUrl(Article article) {
        return baseUrl + "/articles/" + article.getId();
    }

    private ArticleDto toDto(Article article) {
        return ArticleDto.from(article, baseUrl);
    }
}