package br.lcn.ragkb.repository;

import br.lcn.ragkb.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Sector> findAllByOrderByNameAsc();
}