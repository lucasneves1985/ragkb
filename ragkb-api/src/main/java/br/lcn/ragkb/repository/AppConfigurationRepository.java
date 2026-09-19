package br.lcn.ragkb.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.lcn.ragkb.entity.AppConfiguration;

public interface AppConfigurationRepository extends JpaRepository<AppConfiguration, String> {
}