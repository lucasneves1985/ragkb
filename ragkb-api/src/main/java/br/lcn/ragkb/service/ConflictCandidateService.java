package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.ConflictActionRequest;
import br.lcn.ragkb.entity.ConflictCandidate;
import br.lcn.ragkb.entity.ConflictStatus;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.DocumentStatus;
import br.lcn.ragkb.exception.ConflictCandidateNotFoundException;
import br.lcn.ragkb.repository.ConflictCandidateRepository;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConflictCandidateService {

    private final ConflictCandidateRepository conflictRepository;
    private final DocumentMetadataRepository metadataRepository;

    @Transactional
    public ConflictCandidate updateStatus(Long id, ConflictActionRequest request, String username) {
        ConflictCandidate candidate = conflictRepository.findById(id)
                .orElseThrow(() -> new ConflictCandidateNotFoundException(id));

        ConflictStatus newStatus = switch (request.action()) {
            case "REVIEWED" -> ConflictStatus.REVIEWED;
            case "RESOLVED" -> ConflictStatus.RESOLVED;
            case "DISMISSED" -> ConflictStatus.DISMISSED;
            default -> throw new IllegalArgumentException(
                    "Ação inválida: " + request.action() + ". Use REVIEWED, RESOLVED ou DISMISSED");
        };

        if (newStatus == ConflictStatus.RESOLVED) {
            assertResolvable(candidate);
        }

        candidate.updateStatus(newStatus, request.note(), username);
        return conflictRepository.save(candidate);
    }

    private void assertResolvable(ConflictCandidate candidate) {
        DocumentMetadata a = metadataRepository.findById(candidate.getDocumentIdA()).orElse(null);
        DocumentMetadata b = metadataRepository.findById(candidate.getDocumentIdB()).orElse(null);
        boolean aInactive = a == null || a.getStatus() != DocumentStatus.ACTIVE;
        boolean bInactive = b == null || b.getStatus() != DocumentStatus.ACTIVE;
        if (!aInactive && !bInactive) {
            throw new IllegalStateException(
                    "Conflito não resolvido: ambos os documentos ainda estão ACTIVE. " +
                            "Arquive ou superseda um deles antes de marcar RESOLVED.");
        }
    }
}