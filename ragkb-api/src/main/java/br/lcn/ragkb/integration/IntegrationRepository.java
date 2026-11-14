package br.lcn.ragkb.integration;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationRepository extends JpaRepository<Integration, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    List<Integration> findByActiveTrue();

    List<Integration> findByIntegrationTypeAndActiveTrue(IntegrationType type);
}