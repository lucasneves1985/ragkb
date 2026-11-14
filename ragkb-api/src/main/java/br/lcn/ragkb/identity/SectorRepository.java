package br.lcn.ragkb.identity;



import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    Optional<Sector> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Sector> findAllByOrderByNameAsc();
}