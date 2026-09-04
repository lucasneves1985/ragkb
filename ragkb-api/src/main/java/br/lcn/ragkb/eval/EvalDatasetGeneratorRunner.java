package br.lcn.ragkb.eval;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvalDatasetGeneratorRunner implements ApplicationRunner {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final int MIN_WORDS = 8;
    private static final int MAX_WORDS = 40;
    private static final int MAX_PER_DOC = 5;
    private static final Set<String> ANAPHORA = Set.of(
            "ele", "ela", "eles", "elas", "isso", "isto", "este", "esta",
            "esses", "essas", "o mesmo", "a mesma", "tal");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.eval.generate-dataset:false}")
    private boolean enabled;

    @Value("${app.eval.output:src/main/resources/eval/dataset-generated.json}")
    private String outputPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!enabled) {
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT content, metadata FROM vector_store");

        Map<String, DocContext> docs = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String content = (String) row.get("content");
            Map<String, Object> meta = parseMetadata(row.get("metadata"));
            String docId = String.valueOf(meta.get("documentId"));
            String sector = String.valueOf(meta.getOrDefault("sector", "Geral"));
            docs.computeIfAbsent(docId, k -> new DocContext(docId, sector, new ArrayList<>()))
                    .sentences().addAll(extractSentences(content));
        }

        List<EvalItem> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int seq = 0;

        for (DocContext doc : docs.values()) {
            int count = 0;
            for (String sentence : doc.sentences()) {
                if (count >= MAX_PER_DOC) {
                    break;
                }
                for (String question : QuestionGenerator.generate(sentence)) {
                    if (seen.add(question)) {
                        items.add(new EvalItem(
                                "gen-" + (++seq),
                                question,
                                "HAS_ANSWER",
                                doc.docId(),
                                sentence,
                                doc.sector()));
                        count++;
                    }
                }
            }
        }

        Path target = Path.of(outputPath);
        Files.createDirectories(target.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), items);
        log.info("Dataset HAS_ANSWER gerado: {} itens em {}", items.size(), outputPath);
    }

    private Map<String, Object> parseMetadata(Object raw) {
        try {
            // String.valueOf cobre String e PGobject (toString() == getValue()).
            // Evita import de org.postgresql.util.PGobject, que não está no
            // classpath de compilação (driver em escopo runtime).
            String json = String.valueOf(raw);
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> extractSentences(String content) {
        List<String> out = new ArrayList<>();
        for (String raw : SENTENCE_SPLIT.split(content)) {
            String s = raw.trim();
            long words = s.split("\\s+").length;
            if (words < MIN_WORDS || words > MAX_WORDS) {
                continue;
            }
            String lower = s.toLowerCase();
            if (lower.startsWith("http") || lower.matches(".*\\b\\d{4}\\b.*")) {
                continue; // URLs e anos soltos não geram perguntas úteis
            }
            if (ANAPHORA.stream().anyMatch(lower::startsWith)) {
                continue; // pronome anafórico: pergunta ambígua
            }
            out.add(s);
        }
        return out;
    }

    private record DocContext(String docId, String sector, List<String> sentences) {}
}
