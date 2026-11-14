package br.lcn.ragkb.businessrule;


import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.shared.api.SectorUsageCheck;

import lombok.RequiredArgsConstructor;

@Service
@Order(20)
@RequiredArgsConstructor
public class BusinessRuleSectorUsageCheck implements SectorUsageCheck {

    private final BusinessRuleRepository businessRuleRepository;

    @Override
    public Optional<String> conflictMessage(String sectorName) {
        long count = businessRuleRepository.countBySectorNamesContaining(sectorName);
        if (count == 0) {
            return Optional.empty();
        }
        return Optional.of("Setor '" + sectorName + "' é set de acesso de " + count
                + " regra(s) de negócio. Reatribua as regras antes de remover o setor.");
    }
}
