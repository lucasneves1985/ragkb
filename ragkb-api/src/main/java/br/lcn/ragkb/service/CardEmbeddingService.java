package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.lcn.ragkb.entity.Card;
import br.lcn.ragkb.entity.CardChunk;
import br.lcn.ragkb.entity.CardChunkType;
import br.lcn.ragkb.entity.CardComment;
import br.lcn.ragkb.entity.EmbeddingStatus;
import br.lcn.ragkb.repository.CardChunkRepository;
import br.lcn.ragkb.repository.CardCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final CardChunkRepository chunkRepository;
    private final CardCommentRepository commentRepository;

    /**
     * Reconstrói os chunks do card a partir do estado atual (1 chunk CARD + 1
     * chunk por comentário) e embute imediatamente. Falha de embedding NUNCA
     * propaga: chunk fica FAILED e o retry job recupera — mesmo contrato do
     * BusinessRuleEmbeddingService.
     */
    @Transactional
    public void reembedCard(Card card) {
        chunkRepository.deleteByCardId(card.getId());

        chunkRepository.save(new CardChunk(card.getId(), card.getProjectId(),
                CardChunkType.CARD, null, 0,
                card.getTitle() + "\n\n" + (card.getDescription() == null ? "" : card.getDescription())));

        int index = 0;
        for (CardComment comment : commentRepository.findByCardIdOrderByCreatedAtAsc(card.getId())) {
            chunkRepository.save(new CardChunk(card.getId(), card.getProjectId(),
                    CardChunkType.COMMENT, comment.getId(), index++, comment.getContent()));
        }

        for (CardChunk chunk : chunkRepository.findByCardId(card.getId())) {
            embed(chunk);
        }
    }

    /**
     * Embute um chunk. Status machine: PENDING -> DONE on success, PENDING ->
     * FAILED on error. Never throws to the caller.
     */
    @Transactional
    public void embed(CardChunk chunk) {
        try {
            float[] vector = embeddingModel.embed(chunk.getContent());
            chunkRepository.saveEmbedding(chunk.getId(), toLiteral(vector));
        } catch (Exception e) {
            chunkRepository.updateEmbeddingStatus(chunk.getId(), EmbeddingStatus.FAILED);
            log.error("Falha ao gerar embedding do chunk {}: {}", chunk.getId(), e.getMessage());
        }
    }

    public String embedQueryToLiteral(String query) {
        return toLiteral(embeddingModel.embed(query));
    }

    public List<String> semanticSearchChunkIds(String projectId, String query, double similarityThreshold) {
        return chunkRepository.semanticSearchChunkIds(
                projectId, embedQueryToLiteral(query), query, similarityThreshold);
    }

    private String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
