package br.lcn.ragkb.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdCalibrationRunner implements ApplicationRunner {

    private static final int TOP_K = 6;              // mesmo valor do QueryService
    private static final double MIN_T = 0.50;
    private static final double MAX_T = 0.90;
    private static final double STEP = 0.01;
    private static final double FP_COST = 5.0;       // responder errado custa 5x mais
    private static final double FN_COST = 1.0;       // chamado desnecessário custa 1x

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Value("${app.eval.enabled:false}")
    private boolean evalEnabled;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!evalEnabled) {
            return;
        }

        List<EvalItem> items = objectMapper.readValue(
                new ClassPathResource("eval/dataset.json").getInputStream(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, EvalItem.class));

        log.info("Calibracao iniciada: {} itens", items.size());

        // 1) Melhor hit de cada pergunta, SEM threshold (captura o score real)
        List<Result> results = items.stream().map(item -> {
            List<Document> hits = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(item.question())
                            .topK(TOP_K)
                            .build());
            Document best = hits.isEmpty() ? null : hits.get(0);
            return new Result(item, best);
        }).toList();

        // 2) Varre thresholds e calcula metricas
        double bestCost = Double.MAX_VALUE;
        double bestThreshold = MIN_T;

        for (double t = MIN_T; t <= MAX_T; t += STEP) {
            Metrics m = evaluate(results, t);
            if (m.cost() < bestCost) {
                bestCost = m.cost();
                bestThreshold = t;
            }
            log.info("threshold={} precision={} recall={} f1={} fp={} fn={} nearMissAnswered={} cost={}",
                    "%.2f".formatted(t),
                    "%.3f".formatted(m.precision()),
                    "%.3f".formatted(m.recall()),
                    "%.3f".formatted(m.f1()),
                    m.fp(), m.fn(), m.nearMissAnswered(),
                    "%.1f".formatted(m.cost()));
        }

        log.info(">>> Threshold recomendado: {} (custo {})",
                "%.2f".formatted(bestThreshold), "%.1f".formatted(bestCost));
    }

    private Metrics evaluate(List<Result> results, double threshold) {
        int tp = 0, fp = 0, fn = 0, tn = 0, nearMissAnswered = 0;

        for (Result r : results) {
            boolean answered = r.best() != null && r.best().getScore() >= threshold;
            String cls = r.item().expectedClass();

            if ("HAS_ANSWER".equals(cls)) {
                boolean correct = answered
                        && r.best().getMetadata().get("documentId").toString()
                        .equals(r.item().expectedDocId());
                if (correct) tp++;
                else if (answered) fp++;   // respondeu com documento errado: violacao
                else fn++;                 // tinha resposta, sugeriu chamado
            } else if ("NO_ANSWER".equals(cls)) {
                if (answered) fp++;        // respondeu sem ter resposta na base
                else tn++;
            } else { // NEAR_MISS
                if (answered) nearMissAnswered++;  // risco: resposta tangencial
            }
        }

        double precision = tp + fp == 0 ? 0 : (double) tp / (tp + fp);
        double recall = tp + fn == 0 ? 0 : (double) tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double cost = FP_COST * fp + FN_COST * fn;

        return new Metrics(precision, recall, f1, fp, fn, nearMissAnswered, cost);
    }

    private record Result(EvalItem item, Document best) {}
    private record Metrics(double precision, double recall, double f1,
                           int fp, int fn, int nearMissAnswered, double cost) {}
}