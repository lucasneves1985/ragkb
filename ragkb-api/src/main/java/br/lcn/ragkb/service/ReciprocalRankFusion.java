package br.lcn.ragkb.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import br.lcn.ragkb.repository.KbLexicalSearchRepository.LexicalHit;
import lombok.extern.slf4j.Slf4j;

/**
 * Fusão de resultados por Reciprocal Rank Fusion (RRF).
 *
 * <p>score = Σ 1/(k + posição), com posição começando em 1. A fusão é feita
 * POR POSIÇÃO, não por valor de score, porque as escalas são incomensuráveis:
 * cosseno do pgvector varia em [0,1] e ts_rank_cd do PostgreSQL não tem limite
 * superior. Somar diretamente produziria resultados dominados pelo caminho
 * lexical.</p>
 *
 * <p>Contrato de saída: no máximo {@code topK} documentos, sempre. Os três
 * caminhos degenerados (só semântica, só lexical, ambos vazios) respeitam o
 * mesmo limite — sem isso, o caminho lexical puro entregaria
 * {@code lexicalTopK} documentos ao LLM, violando o orçamento de contexto.</p>
 *
 * <p>Determinismo: empates de score são resolvidos pela ordem de inserção no
 * mapa de acumulação, que é {@link LinkedHashMap} — semântica antes, lexical
 * depois. Um {@code HashMap} produziria ordem dependente de hash, e o mesmo
 * conjunto de entrada poderia gerar saídas diferentes entre execuções.</p>
 *
 * <p>A conversão de {@link LexicalHit} para {@link Document} é delegada a uma
 * {@link Function} injetada: o parsing do JSON de metadata (documentId,
 * filename, articleId, articleTitle, articleUrl) vive no QueryService, que já
 * possui o ObjectMapper. A fusão não conhece o formato do metadata — apenas
 * posiciona e ordena.</p>
 *
 * <p>O conversor pode devolver {@code null} para indicar que o hit não é
 * utilizável (ex.: chunk sem content na vector_store). Esses hits são
 * descartados silenciosamente — o log fica a cargo do conversor, que conhece
 * o motivo.</p>
 */
@Slf4j
@Component
public class ReciprocalRankFusion {

    /** Constante clássica do RRF. Valor 60 é o padrão da literatura. */
    static final int RRF_K = 60;

    /**
     * Funde as duas listas e devolve no máximo {@code topK} documentos.
     *
     * @param semanticHits hits do caminho vetorial, já ordenados por similaridade
     * @param lexicalHits  hits do caminho lexical, já ordenados por rank
     * @param topK         limite máximo de documentos no resultado
     * @param lexicalToDocument conversor de LexicalHit para Document com o
     *                     metadata completo; pode devolver null para hits
     *                     não utilizáveis, que são descartados
     * @return lista fundida, ordenada por score RRF decrescente
     */
    public List<Document> fuse(List<Document> semanticHits,
            List<LexicalHit> lexicalHits,
            int topK,
            Function<LexicalHit, Document> lexicalToDocument) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK deve ser positivo, recebido: " + topK);
        }
        if (lexicalToDocument == null) {
            throw new IllegalArgumentException("lexicalToDocument não pode ser nulo");
        }
        List<Document> semantic = semanticHits == null ? List.of() : semanticHits;
        List<LexicalHit> lexical = lexicalHits == null ? List.of() : lexicalHits;

        if (semantic.isEmpty() && lexical.isEmpty()) {
            return List.of();
        }
        // Caminhos degenerados: preservam a ordem original, mas respeitam topK.
        if (lexical.isEmpty()) {
            return semantic.stream().limit(topK).toList();
        }
        if (semantic.isEmpty()) {
            List<Document> converted = new ArrayList<>(lexical.size());
            for (LexicalHit hit : lexical) {
                Document doc = lexicalToDocument.apply(hit);
                if (doc != null) {
                    converted.add(doc);
                }
                if (converted.size() >= topK) {
                    break;
                }
            }
            return converted;
        }

        // LinkedHashMap: ordem de inserção estável → empates determinísticos.
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, Document> docsById = new LinkedHashMap<>();

        for (int i = 0; i < semantic.size(); i++) {
            Document doc = semantic.get(i);
            String id = doc.getId();
            rrfScores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
            docsById.putIfAbsent(id, doc);
        }
        for (int i = 0; i < lexical.size(); i++) {
            LexicalHit hit = lexical.get(i);
            rrfScores.merge(hit.id(), 1.0 / (RRF_K + i + 1), Double::sum);
            if (!docsById.containsKey(hit.id())) {
                Document doc = lexicalToDocument.apply(hit);
                if (doc != null) {
                    docsById.put(hit.id(), doc);
                }
            }
        }

        // sorted() é estável: empates mantêm a ordem de iteração do
        // LinkedHashMap, que é a ordem de inserção (semântica antes, lexical depois).
        List<Document> fused = new ArrayList<>(rrfScores.size());
        rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(entry -> {
                    Document doc = docsById.get(entry.getKey());
                    if (doc != null) {
                        fused.add(doc);
                    }
                });

        log.debug("RRF: {} semântico(s) + {} lexical(is) → {} documento(s)",
                semantic.size(), lexical.size(), fused.size());
        return fused;
    }
}