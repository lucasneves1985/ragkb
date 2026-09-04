package br.lcn.ragkb.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristica deterministica: transforma sentenca factual em perguntas HAS_ANSWER
 * com variacao de wording via sinonimos. Sem chamada de LLM.
 */
final class QuestionGenerator {

    private static final Map<String, String> SYNONYMS = new LinkedHashMap<>(Map.of(
            "redefinir", "resetar",
            "senha", "credencial",
            "prazo", "validade",
            "solicitar", "pedir",
            "acessar", "entrar",
            "colaborador", "funcionario",
            "sistema", "plataforma",
            "necessario", "preciso",
            "enviado", "encaminhado"
    ));

    private static final List<String> STOPWORDS = List.of(
            "o", "a", "os", "as", "um", "uma", "para", "de", "do", "da", "no", "na",
            "em", "com", "por", "ao", "aos", "que", "se", "e", "ou", "ser", "esta");

    private QuestionGenerator() {}

    static List<String> generate(String sentence) {
        String lower = sentence.toLowerCase();
        List<String> questions = new ArrayList<>();

        if (lower.contains("prazo") || lower.contains("validade")) {
            questions.add("Qual o prazo de " + topicOf(lower) + "?");
        }
        if (lower.contains("como") || lower.contains("deve") || lower.contains("necessario")) {
            questions.add("Como " + stripHow(lower) + "?");
        }
        if (lower.contains(" e ") || lower.contains(" é ")) {
            questions.add("O que e " + topicOf(lower) + "?");
        }
        if (questions.isEmpty()) {
            questions.add("O que o documento diz sobre " + topicOf(lower) + "?");
        }

        List<String> out = new ArrayList<>();
        for (String q : questions) {
            out.add(q);
            String variant = applySynonyms(q);
            if (!variant.equals(q)) {
                out.add(variant);
            }
        }
        return out.stream().distinct().toList();
    }

    /** Remove stopwords iniciais e pega os primeiros 3-6 tokens significativos. */
    private static String topicOf(String lower) {
        String[] tokens = lower.split("\s+");
        List<String> kept = new ArrayList<>();
        for (String t : tokens) {
            if (kept.size() >= 6) break;
            if (!STOPWORDS.contains(t)) {
                kept.add(t);
            }
        }
        return String.join(" ", kept);
    }

    /** Remove prefixos comuns de sentenca procedural. */
    private static String stripHow(String lower) {
        for (String prefix : List.of("como ", "o colaborador deve ", "o usuario deve ",
                "para ", "e necessario ", "e preciso ")) {
            if (lower.startsWith(prefix)) {
                return lower.substring(prefix.length());
            }
        }
        return lower;
    }

    private static String applySynonyms(String question) {
        String out = question;
        for (Map.Entry<String, String> e : SYNONYMS.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        return out;
    }
}