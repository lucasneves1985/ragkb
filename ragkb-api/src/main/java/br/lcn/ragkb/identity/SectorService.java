package br.lcn.ragkb.identity;







import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.shared.api.SectorUsageCheck;
import br.lcn.ragkb.shared.exception.SectorInUseException;
import br.lcn.ragkb.shared.exception.SectorNameAlreadyExistsException;
import br.lcn.ragkb.shared.exception.SectorNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final AppUserRepository userRepository;
    private final List<SectorUsageCheck> sectorUsageChecks;

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
            return SectorResponse.from(sectorRepository.save(new Sector(name)));
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
        long users = userRepository.countBySectorId(sector.getId());
        if (users > 0) {
            throw new SectorInUseException("Setor '" + name + "' está vinculado a " + users
                    + " usuário(s). Reatribua os usuários antes de remover o setor.");
        }
        for (SectorUsageCheck check : sectorUsageChecks) {
            check.conflictMessage(name).ifPresent(message -> {
                throw new SectorInUseException(message);
            });
        }
    }
}
