package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.BusinessRuleStatus;
import br.lcn.ragkb.entity.EmbeddingStatus;
import br.lcn.ragkb.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessRuleEmbeddingRetryJob {

    private final BusinessRuleRepository repository;
    private final BusinessRuleEmbeddingService embeddingService;

    /**
     * Re-embeds published rules whose embedding is PENDING or FAILED
     * (e.g. provider was down at publish time). Every 5 minutes.
     * Failure keeps status FAILED — visible in logs, retried next cycle.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void retryPending() {
        List<BusinessRule> pending = repository.findByStatusAndEmbeddingStatusInOrderById(
                BusinessRuleStatus.PUBLISHED, List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED));
        if (pending.isEmpty()) {
            return;
        }
        log.info("Re-embed de {} regra(s) de negócio pendente(s)", pending.size());
        for (BusinessRule rule : pending) {
            embeddingService.embed(rule);
        }
    }
}