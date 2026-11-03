package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.dto.CreateProjectRequest;
import br.lcn.ragkb.dto.ProjectDto;
import br.lcn.ragkb.dto.UpdateProjectRequest;
import br.lcn.ragkb.entity.Project;
import br.lcn.ragkb.exception.DuplicateProjectNameException;
import br.lcn.ragkb.exception.ProjectNotFoundException;
import br.lcn.ragkb.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectDto create(CreateProjectRequest request) {
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateProjectNameException(name);
        }
        return ProjectDto.from(projectRepository.save(new Project(name, request.description(), request.color())));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list() {
        return projectRepository.findAll().stream().map(ProjectDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(String id) {
        return ProjectDto.from(getProject(id));
    }

    @Transactional
    public ProjectDto update(String id, UpdateProjectRequest request) {
        Project project = getProject(id);
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateProjectNameException(name);
        }
        project.updateCore(name, request.description(), request.color());
        return ProjectDto.from(projectRepository.save(project));
    }

    /**
     * A exclusão do projeto cascata para cards, comentários e eventos de status
     * no banco (ON DELETE CASCADE) — não há órfãos.
     */
    @Transactional
    public void delete(String id) {
        projectRepository.delete(getProject(id));
    }

    private Project getProject(String id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
