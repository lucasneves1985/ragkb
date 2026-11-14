package br.lcn.ragkb.integration;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationExecutionRepository extends JpaRepository<IntegrationExecution, String> {

    List<IntegrationExecution> findTop50ByIntegrationIdOrderByStartedAtDesc(String integrationId);
}
