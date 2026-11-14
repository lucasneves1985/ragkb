package br.lcn.ragkb.project;







import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.identity.api.UserLookupApi;
import br.lcn.ragkb.project.api.ProjectLookupApi.ProjectAccess;
import br.lcn.ragkb.project.api.ProjectLookupApi;
import br.lcn.ragkb.shared.exception.DuplicateProjectNameException;
import br.lcn.ragkb.shared.exception.ProjectNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Projetos com compartilhamento (V20). Modelo de permissão:
 * - Owner (projects.created_by): controle total do projeto — editar,
 *   excluir e gerenciar o compartilhamento.
 * - Participante (project_shares): controle total sobre os CARDS do
 *   projeto, exceto exclusão (delete é do criador do card — decisão do
 *   autor). Não gerencia o projeto nem o compartilhamento.
 * - Projeto legado (created_by NULL): visível a todos; edição/exclusão
 *   mantêm o comportamento anterior ao compartilhamento; a privacidade
 *   de cards por criador permanece; NÃO pode ser compartilhado (não tem
 *   owner — para adotá-lo, recrie o projeto).
 */
@Service
@RequiredArgsConstructor
public class ProjectService implements ProjectLookupApi {

    private final ProjectRepository projectRepository;
    private final ProjectShareRepository shareRepository;
    private final UserLookupApi userLookupApi;

    @Override
    public java.util.Optional<ProjectAccess> findVisibleProject(String projectId, Long userId) {
        return projectRepository.findVisibleById(projectId, userId)
                .map(project -> new ProjectAccess(project.getId(), project.getCreatedBy(), project.getName()));
    }

    @Override
    public java.util.Optional<ProjectAccess> findProject(String projectId) {
        return projectRepository.findById(projectId)
                .map(project -> new ProjectAccess(project.getId(), project.getCreatedBy(), project.getName()));
    }

    @Override
    public boolean isParticipant(String projectId, Long userId) {
        return projectRepository.isParticipant(projectId, userId);
    }

    @Transactional
    public ProjectDto create(CreateProjectRequest request, String username) {
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateProjectNameException(name);
        }
        Project project = new Project(name, request.description(), request.color(),
                currentUserId(username));
        return toDto(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list(String username) {
        return projectRepository.findVisibleTo(currentUserId(username)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(String id, String username) {
        return toDto(visibleProject(id, currentUserId(username)));
    }

    @Transactional
    public ProjectDto update(String id, UpdateProjectRequest request, String username) {
        Project project = manageableProject(id, currentUserId(username));
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateProjectNameException(name);
        }
        project.updateCore(name, request.description(), request.color());
        return toDto(projectRepository.save(project));
    }

    /**
     * A exclusão do projeto cascata para cards, comentários e eventos de
     * status no banco (ON DELETE CASCADE) — não há órfãos. Owner-only em
     * projetos owned; legados mantêm o comportamento anterior.
     */
    @Transactional
    public void delete(String id, String username) {
        projectRepository.delete(manageableProject(id, currentUserId(username)));
    }

    // ── Compartilhamento (owner-only) ─────────────────────────

    /**
     * Compartilha o projeto com um usuário (controle total sobre os cards,
     * exceto exclusão). Idempotente: compartilhar duas vezes não duplica.
     * NO PRIMEIRO compartilhamento, as positions do projeto são
     * reindexadas globalmente por status — positions por criador podem
     * colidir (V18 permite) e o board compartilhado exige ordem única.
     */
    @Transactional
    public ProjectDto share(String id, String username, String targetUsername) {
        Project project = ownedProject(id, currentUserId(username));
        Long targetUserId = userLookupApi.findIdByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário '" + targetUsername + "' não encontrado no cadastro."));
        if (targetUserId.equals(project.getCreatedBy())) {
            return toDto(project); // owner já participa por definição
        }
        if (!shareRepository.existsByProjectIdAndUserId(id, targetUserId)) {
            boolean firstShare = shareRepository.findByProjectIdOrderBySharedAtAsc(id).isEmpty();
            shareRepository.save(new ProjectShare(id, targetUserId));
            if (firstShare) {
                projectRepository.reindexPositionsGlobally(id);
            }
        }
        return toDto(project);
    }

    /**
     * Remove um participante. Os cards que ele criou PERMANECEM no projeto
     * (o trabalho pertence ao projeto); ele perde a visibilidade. Owner
     * não está na tabela de shares — remover o próprio username é no-op.
     */
    @Transactional
    public ProjectDto unshare(String id, String username, Long targetUserId) {
        Project project = ownedProject(id, currentUserId(username));
        shareRepository.deleteByProjectIdAndUserId(id, targetUserId);
        return toDto(project);
    }

    @Transactional(readOnly = true)
    public List<String> listShares(String id, String username) {
        ownedProject(id, currentUserId(username));
        List<Long> userIds = shareRepository.findByProjectIdOrderBySharedAtAsc(id).stream()
                .map(ProjectShare::getUserId)
                .toList();
        Map<Long, String> usernames = userLookupApi.findUsernamesByIds(userIds);
        return userIds.stream().map(uid -> usernames.getOrDefault(uid, "?")).toList();
    }

    // ── Resolução de acesso ───────────────────────────────────

    /** Projeto visível ao usuário (owner, participante ou legado público). */
    private Project visibleProject(String id, Long userId) {
        return projectRepository.findVisibleById(id, userId)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    /**
     * Projeto gerenciável (editar/excluir): owner-only em projetos owned;
     * legados (created_by NULL) mantêm o comportamento anterior. Violação
     * responde 404, não 403 — não revela a existência do projeto alheio.
     */
    private Project manageableProject(String id, Long userId) {
        Project project = visibleProject(id, userId);
        if (project.getCreatedBy() != null && !project.getCreatedBy().equals(userId)) {
            throw new ProjectNotFoundException(id);
        }
        return project;
    }

    /** Projeto cujo compartilhamento será gerenciado: owner exclusivamente. */
    private Project ownedProject(String id, Long userId) {
        Project project = visibleProject(id, userId);
        if (project.getCreatedBy() == null || !project.getCreatedBy().equals(userId)) {
            throw new ProjectNotFoundException(id);
        }
        return project;
    }

    /**
     * Resolve o id do AppUser autenticado a partir do username (subject do
     * JWT). Padrão do projeto: username como parâmetro (UserService).
     */
    private Long currentUserId(String username) {
        return userLookupApi.findIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário autenticado não encontrado: " + username));
    }

    private ProjectDto toDto(Project project) {
        String ownerUsername = project.getCreatedBy() == null ? null
                : userLookupApi.findUsernameById(project.getCreatedBy()).orElse(null);
        List<Long> userIds = shareRepository.findByProjectIdOrderBySharedAtAsc(project.getId()).stream()
                .map(ProjectShare::getUserId)
                .toList();
        Map<Long, String> usernames = userLookupApi.findUsernamesByIds(userIds);
        List<String> participants = userIds.stream().map(id -> usernames.getOrDefault(id, "?")).toList();
        return ProjectDto.from(project, ownerUsername, participants);
    }
}
