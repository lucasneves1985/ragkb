package br.lcn.ragkb.repository;

import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {
    boolean existsByContentHash(String contentHash);
    List<DocumentMetadata> findAllByStatus(DocumentStatus status);


    @Query("select count(d) from DocumentMetadata d where :sector member of d.allowedSectors")
    long countByAllowedSectorsContaining(@Param("sector") String sector);
}