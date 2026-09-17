package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.ArticleDetailDto;
import br.lcn.ragkb.dto.ArticleDto;
import br.lcn.ragkb.dto.CreateArticleRequest;
import br.lcn.ragkb.dto.UpdateArticleRequest;
import br.lcn.ragkb.entity.Article;
import br.lcn.ragkb.entity.ArticleStatus;
import br.lcn.ragkb.exception.ArticleNotFoundException;
import br.lcn.ragkb.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;

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

    /**
     * Detail view — read-only page (chat source link, portal) and editor dialog.
     * Visibility mirrors the vector-store sector filter that allowed the
     * agent to cite the article in the first place:
     * - ADMIN: everything.
     * - Author: own articles in any status.
     * - Any authenticated user: PUBLISHED articles whose allowedSectors
     *   include the user's sector.
     * Anything else: 404 (does not reveal existence).
     */
    @Transactional(readOnly = true)
    public ArticleDetailDto getDetail(String id, String username, boolean isAdmin) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException(id));

        if (isAdmin || article.getAuthorUsername().equals(username)) {
            return ArticleDetailDto.from(article, baseUrl);
        }

        String userSector = userService.findByUsername(username);
        if (article.isPublished() && userSector != null
                && article.getAllowedSectors().contains(userSector)) {
            return ArticleDetailDto.from(article, baseUrl);
        }

        throw new ArticleNotFoundException(id);
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

    /**
     * Portal feed — read-only, for ALL authenticated users (incl. ROLE_USER).
     * Only PUBLISHED articles. Non-admin users are always bound to their own
     * sector (the requested sector filter is ignored for them); ADMIN may
     * filter by any sector or see all. Optional title search (q).
     */
    @Transactional(readOnly = true)
    public List<ArticleDetailDto> listPortal(String username, boolean isAdmin, String q, String sector) {
        String search = (q == null || q.isBlank()) ? null : q.trim();

        List<Article> articles;
        if (isAdmin) {
            String effectiveSector = (sector == null || sector.isBlank()) ? null : sector.trim();
            articles = queryPublished(effectiveSector, search);
        } else {
            String userSector = userService.findByUsername(username);
            if (userSector == null || userSector.isBlank()) {
                return List.of();
            }
            articles = queryPublished(userSector, search);
        }

        return articles.stream()
                .map(a -> ArticleDetailDto.from(a, baseUrl))
                .toList();
    }

    private List<Article> queryPublished(String sector, String search) {
        if (sector != null && search != null) {
            return articleRepository
                    .findByStatusAndAllowedSectorsContainingAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                            ArticleStatus.PUBLISHED, sector, search);
        }
        if (sector != null) {
            return articleRepository
                    .findByStatusAndAllowedSectorsContainingOrderByPublishedAtDesc(
                            ArticleStatus.PUBLISHED, sector);
        }
        if (search != null) {
            return articleRepository
                    .findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                            ArticleStatus.PUBLISHED, search);
        }
        return articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
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