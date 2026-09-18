package br.lcn.ragkb.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import br.lcn.ragkb.entity.BusinessRule;
import br.lcn.ragkb.entity.EmbeddingStatus;
import br.lcn.ragkb.repository.BusinessRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessRuleEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final BusinessRuleRepository repository;

    /**
     * Embeds title + description into the rule's own embedding column.
     * Status machine: PENDING -> DONE on success, PENDING -> FAILED on error.
     * FAILED distinguishes "provider was down" from "never attempted" and
     * lets the retry job pick it up again. Never throws to the caller:
     * a failed embedding must not roll back the save or block the publish.
     */
    public void embed(BusinessRule rule) {
        try {
            float[] vector = embeddingModel.embed(rule.getTitle() + "\n\n" + rule.getDescription());
            repository.saveEmbedding(rule.getId(), toLiteral(vector));
        } catch (Exception e) {
            repository.updateEmbeddingStatus(rule.getId(), EmbeddingStatus.FAILED);
            log.error("Falha ao gerar embedding da regra {}: {}", rule.getId(), e.getMessage());
        }
    }

    public String embedQueryToLiteral(String query) {
        return toLiteral(embeddingModel.embed(query));
    }

    private String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}