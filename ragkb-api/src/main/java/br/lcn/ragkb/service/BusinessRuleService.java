package br.lcn.ragkb.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.BusinessRuleDto;
import br.lcn.ragkb.dto.CreateBusinessRuleRequest;
import br.lcn.ragkb.dto.UpdateBusinessRuleRequest;
import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.BusinessRuleStatus;
import br.lcn.ragkb.exception.BusinessRuleNotFoundException;
import br.lcn.ragkb.exception.DuplicateBusinessRuleTitleException;
import br.lcn.ragkb.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessRuleService {

    private final BusinessRuleRepository repository;
    private final BusinessRuleEmbeddingService embeddingService;
    private final UserService userService;

    @Transactional
    public BusinessRuleDto create(CreateBusinessRuleRequest request, String username, boolean isAdmin) {
        String title = request.title().trim();
        if (repository.existsByTitleIgnoreCase(title)) {
            throw new DuplicateBusinessRuleTitleException(title);
        }

        // Regra nasce DRAFT, igual artigo: status, embedding_status = PENDING
        // e version = 0 são inicializados no construtor da entidade.
        // Draft NÃO recebe embedding — só ao publicar.
        BusinessRule rule = new BusinessRule(
                title,
                request.description().trim(),
                request.requester(),
                request.reason(),
                username,
                request.sectorNames(),
                request.articleIds());

        return BusinessRuleDto.from(repository.save(rule));
    }

    /**
     * Detail view — visibility mirrors ArticleLifecycleService.getDetail:
     * - ADMIN: everything.
     * - Author: own rules in any status.
     * - Any authenticated user: PUBLISHED rules whose sectorNames include
     *   the user's sector (AppUser has a single sector — sectorId).
     * Anything else: 404 (does not reveal existence).
     */
    @Transactional(readOnly = true)
    public BusinessRuleDto getDetail(String id, String username, boolean isAdmin) {
        BusinessRule rule = find(id);
        if (isAdmin || rule.getAuthorUsername().equals(username)) {
            return BusinessRuleDto.from(rule);
        }

        String userSector = userService.findByUsername(username);
        if (rule.isPublished() && userSector != null
                && rule.getSectorNames().contains(userSector)) {
            return BusinessRuleDto.from(rule);
        }

        throw new BusinessRuleNotFoundException(id);
    }

    /**
     * Same visibility gate as getDetail — used by the attachment endpoints
     * (list/download) so the rule-attachment surfaces never bypass the
     * sector permission model.
     */
    @Transactional(readOnly = true)
    public void assertReadable(String id, String username, boolean isAdmin) {
        getDetail(id, username, isAdmin);
    }

    /**
     * Update of content. Re-embedding ONLY when the rule is published AND
     * title/description actually changed (status toggle does not embed).
     * Failure is not fatal: embed() keeps the rule with FAILED status and
     * the retry job recovers it.
     */
    @Transactional
    public BusinessRuleDto update(String id, UpdateBusinessRuleRequest request, String username, boolean isAdmin) {
        BusinessRule rule = findOwned(id, username, isAdmin);

        String title = request.title().trim();
        if (repository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
            throw new DuplicateBusinessRuleTitleException(title);
        }

        boolean reindex = rule.isPublished()
                && (!rule.getTitle().equals(title) || !rule.getDescription().equals(request.description()));

        // Autoria de alteração sempre do security context — nunca do request.
        rule.updateCore(title, request.description().trim(), request.requester(),
                request.reason(), username, request.sectorNames(), request.articleIds());
        repository.save(rule);

        if (reindex) {
            embeddingService.embed(rule);
        }

        return BusinessRuleDto.from(rule);
    }

    /**
     * DRAFT or ARCHIVED -> PUBLISHED (toggle bidirecional combinado na spec).
     * Re-publishing re-embeds: the archived period may have outlived a
     * description change, so the vector is refreshed unconditionally.
     * A provider failure does NOT roll back the publish (FAILED + retry job).
     */
    @Transactional
    public BusinessRuleDto publish(String id, String username, boolean isAdmin) {
        BusinessRule rule = findOwned(id, username, isAdmin);
        if (rule.isPublished()) {
            throw new IllegalStateException("A regra já está publicada");
        }

        rule.publish();
        repository.saveAndFlush(rule);

        embeddingService.embed(rule);

        return BusinessRuleDto.from(
                repository.findById(id).orElseThrow(() -> new BusinessRuleNotFoundException(id)));
    }

    /**
     * PUBLISHED -> ARCHIVED. Unlike articles (which delete vectors from the
     * shared vector_store), the embedding column is simply filtered out by
     * status = 'PUBLISHED' in every semantic query, so no cleanup is needed —
     * and re-publishing re-embeds on publish() anyway.
     */
    @Transactional
    public BusinessRuleDto archive(String id, String username, boolean isAdmin) {
        BusinessRule rule = findOwned(id, username, isAdmin);
        rule.archive();
        return BusinessRuleDto.from(repository.save(rule));
    }

    /**
     * Management listing — mirrors ArticleLifecycleService.list:
     * - ADMIN: everything.
     * - EDITOR: published/archived plus own drafts (never others').
     */
    @Transactional(readOnly = true)
    public List<BusinessRuleDto> list(String username, boolean isAdmin) {
        List<BusinessRule> rules = isAdmin
                ? repository.findAll()
                : repository.findByStatusNotOrAuthorUsername(BusinessRuleStatus.DRAFT, username);
        return rules.stream().map(BusinessRuleDto::from).toList();
    }

    /**
     * Portal feed — read-only, for ALL authenticated users (incl. ROLE_USER).
     * Only PUBLISHED rules. Non-admin users are always bound to their own
     * sector; ADMIN may see all sectors. With q: semantic search over
     * title+description embeddings (permission filter BEFORE ranking).
     * Without q: recency order (published_at desc).
     */
    @Transactional(readOnly = true)
    public List<BusinessRuleDto> listPortal(String username, boolean isAdmin, String q) {
        String sector = isAdmin ? null : userService.findByUsername(username);
        if (!isAdmin && (sector == null || sector.isBlank())) {
            return List.of();
        }
        String query = (q == null || q.isBlank()) ? null : q.trim();

        if (query == null) {
            List<BusinessRule> rules = (sector == null)
                    ? repository.findByStatusOrderByPublishedAtDesc(BusinessRuleStatus.PUBLISHED)
                    : repository.findByStatusAndSectorNamesContainingOrderByPublishedAtDesc(
                            BusinessRuleStatus.PUBLISHED, sector);
            return rules.stream().map(BusinessRuleDto::from).toList();
        }

        // Busca semântica: permissão aplicada no próprio SQL, antes do ranking.
        String queryVector = embeddingService.embedQueryToLiteral(query);
        List<String> ids = repository.semanticSearchIds(sector, queryVector);
        Map<String, BusinessRule> byId = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(BusinessRule::getId, Function.identity()));

        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(BusinessRuleDto::from)
                .toList();
    }

    private BusinessRule find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleNotFoundException(id));
    }

    /** Ownership gate — non-admin without ownership gets 404, not 403. */
    private BusinessRule findOwned(String id, String username, boolean isAdmin) {
        BusinessRule rule = find(id);
        if (!isAdmin && !rule.getAuthorUsername().equals(username)) {
            throw new BusinessRuleNotFoundException(id);
        }
        return rule;
    }
}