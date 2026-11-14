package br.lcn.ragkb.knowledge;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {
    boolean existsByContentHash(String contentHash);
    List<DocumentMetadata> findAllByStatus(DocumentStatus status);


    @Query("select count(d) from DocumentMetadata d where :sector member of d.allowedSectors")
    long countByAllowedSectorsContaining(@Param("sector") String sector);
}