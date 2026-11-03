package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.entity.CardChunk;
import br.lcn.ragkb.entity.EmbeddingStatus;
import br.lcn.ragkb.repository.CardChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardEmbeddingRetryJob {

    private final CardChunkRepository chunkRepository;
    private final CardEmbeddingService embeddingService;

    /**
     * Re-embeds chunks whose embedding is PENDING or FAILED (e.g. provider was
     * down at write time). Every 5 minutes. Failure keeps status FAILED —
     * visible in logs, retried next cycle.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    @Transactional
    public void retryPending() {
        List<CardChunk> pending = chunkRepository.findByEmbeddingStatusInOrderById(
                List.of(EmbeddingStatus.PENDING, EmbeddingStatus.FAILED));
        if (pending.isEmpty()) {
            return;
        }
        log.info("Re-embed de {} chunk(s) de card pendente(s)", pending.size());
        for (CardChunk chunk : pending) {
            embeddingService.embed(chunk);
        }
    }
}
