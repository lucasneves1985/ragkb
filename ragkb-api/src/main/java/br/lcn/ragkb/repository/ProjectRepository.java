package br.lcn.ragkb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}
