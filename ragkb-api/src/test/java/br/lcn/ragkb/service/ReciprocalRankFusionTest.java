package br.lcn.ragkb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import br.lcn.ragkb.repository.KbLexicalSearchRepository.LexicalHit;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion();

    /**
     * Conversor de teste: simula o que o QueryService.toDocument faz, mas sem
     * o parsing de JSON. O parsing é responsabilidade do QueryService — a
     * fusão só precisa de um Document com id e texto.
     */
    private static final Function<LexicalHit, Document> CONVERSOR = hit ->
            Document.builder()
                    .id(hit.id())
                    .text(hit.content())
                    .metadata(Map.of("origem", "lexical"))
                    .build();

    /** Conversor que devolve null para ids começando com "vazio". */
    private static final Function<LexicalHit, Document> CONVERSOR_COM_NULOS = hit ->
            hit.id().startsWith("vazio")
                    ? null
                    : Document.builder()
                            .id(hit.id())
                            .text(hit.content())
                            .metadata(Map.of("origem", "lexical"))
                            .build();

    private static Document doc(String id) {
        return Document.builder()
                .id(id)
                .text("conteúdo " + id)
                .metadata(Map.of("origem", "semantica"))
                .build();
    }

    private static LexicalHit hit(String id) {
        return new LexicalHit(id, "conteúdo " + id, "{}", 0.5);
    }

    @Test
    @DisplayName("ambas vazias → resultado vazio")
    void ambasVazias() {
        assertThat(fusion.fuse(List.of(), List.of(), 6, CONVERSOR)).isEmpty();
    }

    @Test
    @DisplayName("só semântica → preserva ordem e respeita topK")
    void soSemantica() {
        List<Document> semantic = List.of(doc("a"), doc("b"), doc("c"));

        List<Document> result = fusion.fuse(semantic, List.of(), 2, CONVERSOR);

        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }

    @Test
    @DisplayName("só lexical → respeita topK (não vaza lexicalTopK)")
    void soLexical() {
        List<LexicalHit> lexical = List.of(hit("a"), hit("b"), hit("c"), hit("d"));

        List<Document> result = fusion.fuse(List.of(), lexical, 2, CONVERSOR);

        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }

    @Test
    @DisplayName("documento nas duas listas é promovido pelo consenso")
    void consensoVence() {
        // "b" está em 2º semântico e 1º lexical → soma dos dois scores
        List<Document> semantic = List.of(doc("a"), doc("b"));
        List<LexicalHit> lexical = List.of(hit("b"), hit("c"));

        List<Document> result = fusion.fuse(semantic, lexical, 6, CONVERSOR);

        assertThat(result).extracting(Document::getId).containsExactly("b", "a", "c");
    }

    @Test
    @DisplayName("score RRF usa posição, não valor de rank")
    void scorePorPosicao() {
        List<LexicalHit> lexical = List.of(
                new LexicalHit("a", "c", "{}", 0.001),
                new LexicalHit("b", "c", "{}", 999.0));

        List<Document> result = fusion.fuse(List.of(), lexical, 6, CONVERSOR);

        // rank 999 não domina: a ordem é por posição
        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }

    @Test
    @DisplayName("empate de score é determinístico entre execuções")
    void empateDeterministico() {
        List<Document> semantic = List.of(doc("x"));
        List<LexicalHit> lexical = List.of(hit("y"));

        // x e y têm score idêntico (1/61). Ordem deve ser sempre x antes de y.
        for (int i = 0; i < 50; i++) {
            List<Document> result = fusion.fuse(semantic, lexical, 6, CONVERSOR);
            assertThat(result).extracting(Document::getId).containsExactly("x", "y");
        }
    }

    @Test
    @DisplayName("hit duplicado entre listas não gera documento duplicado")
    void semDuplicata() {
        List<Document> semantic = List.of(doc("a"));
        List<LexicalHit> lexical = List.of(hit("a"));

        List<Document> result = fusion.fuse(semantic, lexical, 6, CONVERSOR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("a");
    }

    @Test
    @DisplayName("metadata semântica prevalece sobre a lexical")
    void semanticaPrevalece() {
        List<Document> result = fusion.fuse(List.of(doc("a")), List.of(hit("a")), 6, CONVERSOR);

        assertThat(result.get(0).getMetadata()).containsEntry("origem", "semantica");
    }

    @Test
    @DisplayName("topK inválido lança exceção")
    void topKInvalido() {
        assertThatThrownBy(() -> fusion.fuse(List.of(), List.of(), 0, CONVERSOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("conversor nulo lança exceção")
    void conversorNulo() {
        assertThatThrownBy(() -> fusion.fuse(List.of(), List.of(), 6, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("resultado nunca excede topK, em qualquer caminho")
    void nuncaExcedeTopK() {
        List<Document> semantic = List.of(doc("a"), doc("b"), doc("c"), doc("d"), doc("e"));
        List<LexicalHit> lexical = List.of(hit("f"), hit("g"), hit("h"), hit("i"), hit("j"));

        assertThat(fusion.fuse(semantic, List.of(), 3, CONVERSOR)).hasSize(3);
        assertThat(fusion.fuse(List.of(), lexical, 3, CONVERSOR)).hasSize(3);
        assertThat(fusion.fuse(semantic, lexical, 3, CONVERSOR)).hasSize(3);
    }

    @Test
    @DisplayName("hit convertido para null é descartado no caminho lexical puro")
    void descartaNulosLexicalPuro() {
        List<LexicalHit> lexical = List.of(hit("vazio-1"), hit("a"), hit("vazio-2"), hit("b"));

        List<Document> result = fusion.fuse(List.of(), lexical, 6, CONVERSOR_COM_NULOS);

        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }

    @Test
    @DisplayName("hit convertido para null é descartado no caminho fundido")
    void descartaNulosFundido() {
        List<Document> semantic = List.of(doc("s1"));
        List<LexicalHit> lexical = List.of(hit("vazio-1"), hit("s1"), hit("l1"));

        List<Document> result = fusion.fuse(semantic, lexical, 6, CONVERSOR_COM_NULOS);

        assertThat(result).extracting(Document::getId).containsExactly("s1", "l1");
    }

    @Test
    @DisplayName("topK é respeitado mesmo com nulos descartados")
    void topKComNulos() {
        List<LexicalHit> lexical = List.of(
                hit("vazio-1"), hit("a"), hit("vazio-2"), hit("b"), hit("c"));

        List<Document> result = fusion.fuse(List.of(), lexical, 2, CONVERSOR_COM_NULOS);

        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }
}