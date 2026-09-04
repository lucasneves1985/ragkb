package br.lcn.ragkb.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SectorClassifier {

    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "TI", List.of("sistema", "login", "senha", "erro", "bug", "acesso"),
            "RH", List.of("ferias", "salario", "holerite", "admissao", "rescisao"),
            "Financeiro", List.of("nota fiscal", "pagamento", "reembolso", "fatura", "boleto"),
            "Juridico", List.of("contrato", "clausula", "processo", "acordo")
    );

    public String classify(String question) {
        String q = question.toLowerCase();
        return KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(q::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("Suporte Geral");
    }
}