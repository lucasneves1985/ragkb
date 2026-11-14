package br.lcn.ragkb.knowledge;


import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.shared.api.SectorUsageCheck;

import lombok.RequiredArgsConstructor;

@Service
@Order(10)
@RequiredArgsConstructor
public class KnowledgeSectorUsageCheck implements SectorUsageCheck {

    private final DocumentMetadataRepository documentRepository;

    @Override
    public Optional<String> conflictMessage(String sectorName) {
        long asOwner = documentRepository.countByAllowedSectorsContaining(sectorName);
        if (asOwner > 0) {
            return Optional.of("Setor '" + sectorName + "' é o setor responsável de " + asOwner
                    + " documento(s). Reatribua os documentos antes de remover o setor.");
        }
        long asAllowed = documentRepository.countByAllowedSectorsContaining(sectorName);
        if (asAllowed > 0) {
            return Optional.of("Setor '" + sectorName + "' consta nos setores com acesso de " + asAllowed
                    + " documento(s). Remova esse acesso antes de excluir o setor.");
        }
        return Optional.empty();
    }
}
