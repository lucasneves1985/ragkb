package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.Integration;
import br.lcn.ragkb.entity.IntegrationType;

public interface IntegrationRepository extends JpaRepository<Integration, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    List<Integration> findByActiveTrue();

    List<Integration> findByIntegrationTypeAndActiveTrue(IntegrationType type);
}