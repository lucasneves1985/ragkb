package br.lcn.ragkb.service;

import br.lcn.ragkb.dto.CreateSectorRequest;
import br.lcn.ragkb.dto.SectorResponse;
import br.lcn.ragkb.entity.Sector;
import br.lcn.ragkb.exception.SectorInUseException;
import br.lcn.ragkb.exception.SectorNameAlreadyExistsException;
import br.lcn.ragkb.exception.SectorNotFoundException;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import br.lcn.ragkb.repository.SectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final AppUserRepository userRepository;
    private final DocumentMetadataRepository documentRepository;

    @Transactional(readOnly = true)
    public List<SectorResponse> listAll() {
        return sectorRepository.findAllByOrderByNameAsc().stream()
                .map(SectorResponse::from)
                .toList();
    }

    @Transactional
    public SectorResponse create(CreateSectorRequest request) {
        String name = request.name().trim();
        if (sectorRepository.existsByNameIgnoreCase(name)) {
            throw new SectorNameAlreadyExistsException(name);
        }
        try {
            Sector saved = sectorRepository.save(new Sector(name));
            return SectorResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Corrida: unique constraint disparou antes do exists
            throw new SectorNameAlreadyExistsException(name);
        }
    }

    @Transactional
    public void delete(Long id) {
        Sector sector = sectorRepository.findById(id)
                .orElseThrow(() -> new SectorNotFoundException(id));
        assertNotInUse(sector);
        sectorRepository.delete(sector);
    }

    private void assertNotInUse(Sector sector) {
        String name = sector.getName();

        long users = userRepository.countBySector(sector.getName());
        if (users > 0) {
            throw new SectorInUseException("Setor '" + name + "' está vinculado a " + users
                    + " usuário(s). Reatribua os usuários antes de remover o setor.");
        }

        long asOwner = documentRepository.countByAllowedSectorsContaining(name);
        if (asOwner > 0) {
            throw new SectorInUseException("Setor '" + name + "' é o setor responsável de " + asOwner
                    + " documento(s). Reatribua os documentos antes de remover o setor.");
        }

        long asAllowed = documentRepository.countByAllowedSectorsContaining(name);
        if (asAllowed > 0) {
            throw new SectorInUseException("Setor '" + name + "' consta nos setores com acesso de " + asAllowed
                    + " documento(s). Remova esse acesso antes de excluir o setor.");
        }
    }
}