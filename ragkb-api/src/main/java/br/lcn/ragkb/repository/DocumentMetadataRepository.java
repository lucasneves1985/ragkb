package br.lcn.ragkb.repository;

import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {
    boolean existsByContentHash(String contentHash);
    List<DocumentMetadata> findAllByStatus(DocumentStatus status);
}