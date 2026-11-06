package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.CreateProjectRequest;
import br.lcn.ragkb.dto.ProjectDto;
import br.lcn.ragkb.dto.UpdateProjectRequest;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.Project;
import br.lcn.ragkb.entity.ProjectShare;
import br.lcn.ragkb.exception.DuplicateProjectNameException;
import br.lcn.ragkb.exception.ProjectNotFoundException;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.ProjectRepository;
import br.lcn.ragkb.repository.ProjectShareRepository;
import lombok.RequiredArgsConstructor;

/**
 * Projetos com compartilhamento (V20). Modelo de permissão: - Owner
 * (projects.created_by): controle total do projeto — editar, excluir e
 * gerenciar o compartilhamento. - Participante (project_shares): controle total
 * sobre os CARDS do projeto, exceto exclusão (delete é do criador do card —
 * decisão do autor). Não gerencia o projeto nem o compartilhamento. - Projeto
 * legado (created_by NULL): visível a todos; edição/exclusão mantêm o
 * comportamento anterior ao compartilhamento; a privacidade de cards por
 * criador permanece; NÃO pode ser compartilhado (não tem owner — para adotá-lo,
 * recrie o projeto).
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectShareRepository shareRepository;
    private final AppUserRepository userRepository;

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
     * A exclusão do projeto cascata para cards, comentários e eventos de status
     * no banco (ON DELETE CASCADE) — não há órfãos. Owner-only em projetos
     * owned; legados mantêm o comportamento anterior.
     */
    @Transactional
    public void delete(String id, String username) {
        projectRepository.delete(manageableProject(id, currentUserId(username)));
    }

    // ── Compartilhamento (owner-only) ─────────────────────────
    /**
     * Compartilha o projeto com um usuário (controle total sobre os cards,
     * exceto exclusão). Idempotente: compartilhar duas vezes não duplica. NO
     * PRIMEIRO compartilhamento, as positions do projeto são reindexadas
     * globalmente por status — positions por criador podem colidir (V18
     * permite) e o board compartilhado exige ordem única.
     */
    @Transactional
    public ProjectDto share(String id, String username, String targetUsername) {
        Project project = ownedProject(id, currentUserId(username));
        AppUser target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário '" + targetUsername + "' não encontrado no cadastro."));
        if (target.getId().equals(project.getCreatedBy())) {
            return toDto(project); // owner já participa por definição
        }
        if (!shareRepository.existsByProjectIdAndUserId(id, target.getId())) {
            boolean firstShare = shareRepository.findByProjectIdOrderBySharedAtAsc(id).isEmpty();
            shareRepository.save(new ProjectShare(id, target.getId()));
            if (firstShare) {
                projectRepository.reindexPositionsGlobally(id);
            }
        }
        return toDto(project);
    }

    /**
     * Remove um participante. Os cards que ele criou PERMANECEM no projeto (o
     * trabalho pertence ao projeto); ele perde a visibilidade. Owner não está
     * na tabela de shares — remover o próprio username é no-op.
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
        return shareRepository.findByProjectIdOrderBySharedAtAsc(id).stream()
                .map(ProjectShare::getUserId)
                .map(uid -> userRepository.findById(uid).map(AppUser::getUsername).orElse("?"))
                .toList();
    }

    // ── Resolução de acesso ───────────────────────────────────
    /**
     * Projeto visível ao usuário (owner, participante ou legado público).
     */
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

    /**
     * Projeto cujo compartilhamento será gerenciado: owner exclusivamente.
     */
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
        return userRepository.findByUsername(username)
                .map(AppUser::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuário autenticado não encontrado: " + username));
    }

    private ProjectDto toDto(Project project) {
        String ownerUsername = project.getCreatedBy() == null ? null
                : userRepository.findById(project.getCreatedBy())
                        .map(AppUser::getUsername).orElse(null);
        List<String> participants = shareRepository
                .findByProjectIdOrderBySharedAtAsc(project.getId()).stream()
                .map(s -> userRepository.findById(s.getUserId())
                        .map(AppUser::getUsername).orElse("?"))
                .toList();
        return ProjectDto.from(project, ownerUsername, participants);
    }
}
