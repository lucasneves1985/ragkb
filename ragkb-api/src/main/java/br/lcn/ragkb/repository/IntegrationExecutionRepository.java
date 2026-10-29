package br.lcn.ragkb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.IntegrationExecution;

public interface IntegrationExecutionRepository extends JpaRepository<IntegrationExecution, String> {

    List<IntegrationExecution> findTop50ByIntegrationIdOrderByStartedAtDesc(String integrationId);
}
