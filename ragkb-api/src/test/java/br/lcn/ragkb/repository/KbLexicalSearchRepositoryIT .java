package br.lcn.ragkb.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import br.lcn.ragkb.repository.KbLexicalSearchRepository.LexicalHit;
import br.lcn.ragkb.support.IntegrationTestBase;

/**
 * Teste de integração do filtro de permissões da busca lexical.
 *
 * <p>Este é o teste de maior risco do conjunto: um erro em rolesFilter() ou no
 * jsonb_exists_any() faz um chunk proibido aparecer no resultado do LLM —
 * vazamento de dado, não bug de produto.</p>
 *
 * <p>Os dados são inseridos diretamente na vector_store, sem passar pelo
 * pipeline de ingestão. Isso é intencional: o teste valida o SQL do filtro, não
 * o pipeline. Inserir via IngestionService exigiria EmbeddingModel real.</p>
 *
 * <p>Nota sobre content_tsv: é coluna GERADA por to_tsvector('portuguese',
 * content). Não se insere nela. O stemming português é aplicado
 * automaticamente — por isso os termos do teste usam variação morfológica real
 * ("contratos" no conteúdo, "contrato" na busca).</p>
 */
class KbLexicalSearchRepositoryIT extends IntegrationTestBase {

    @Autowired
    private KbLexicalSearchRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID CHUNK_PUBLICO = UUID.randomUUID();
    private static final UUID CHUNK_ROLE_USER = UUID.randomUUID();
    private static final UUID CHUNK_ROLE_EDITOR = UUID.randomUUID();
    private static final UUID CHUNK_SETOR_FINANCEIRO = UUID.randomUUID();
    private static final UUID CHUNK_SETOR_JURIDICO = UUID.randomUUID();

    @BeforeEach
    void limparEInserirFixtures() {
        jdbcTemplate.execute("DELETE FROM vector_store");

        // Chunk visível a todos os roles e setores
        inserir(CHUNK_PUBLICO,
                "O contrato de fornecimento deve ser revisado anualmente pelo jurídico.",
                """
                {"allowedRoles": ["ROLE_USER", "ROLE_EDITOR", "ROLE_ADMIN"],
                 "allowedSectors": ["financeiro", "juridico"]}
                """);

        // Chunk restrito a ROLE_USER
        inserir(CHUNK_ROLE_USER,
                "O contrato de prestação de serviços exige aprovação do gestor.",
                """
                {"allowedRoles": ["ROLE_USER"],
                 "allowedSectors": ["financeiro", "juridico"]}
                """);

        // Chunk restrito a ROLE_EDITOR
        inserir(CHUNK_ROLE_EDITOR,
                "O contrato de licenciamento de software tem cláusula de renovação.",
                """
                {"allowedRoles": ["ROLE_EDITOR"],
                 "allowedSectors": ["financeiro", "juridico"]}
                """);

        // Chunk restrito ao setor financeiro
        inserir(CHUNK_SETOR_FINANCEIRO,
                "O contrato de câmbio é registrado no sistema financeiro.",
                """
                {"allowedRoles": ["ROLE_USER", "ROLE_EDITOR", "ROLE_ADMIN"],
                 "allowedSectors": ["financeiro"]}
                """);

        // Chunk restrito ao setor jurídico
        inserir(CHUNK_SETOR_JURIDICO,
                "O contrato de honorários advocatícios é arquivado no jurídico.",
                """
                {"allowedRoles": ["ROLE_USER", "ROLE_EDITOR", "ROLE_ADMIN"],
                 "allowedSectors": ["juridico"]}
                """);
    }

    private void inserir(UUID id, String content, String metadataJson) {
        jdbcTemplate.update(
                "INSERT INTO vector_store (id, content, metadata) VALUES (?, ?, CAST(? AS json))",
                id, content, metadataJson);
    }

    @Test
    @DisplayName("ROLE_USER no setor financeiro vê apenas chunks permitidos")
    void roleUserFinanceiro() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_USER"), "financeiro", false, 10);

        assertThat(hits).extracting(LexicalHit::id)
                .containsExactlyInAnyOrder(
                        CHUNK_PUBLICO.toString(),
                        CHUNK_ROLE_USER.toString(),
                        CHUNK_SETOR_FINANCEIRO.toString())
                .doesNotContain(
                        CHUNK_ROLE_EDITOR.toString(),
                        CHUNK_SETOR_JURIDICO.toString());
    }

    @Test
    @DisplayName("ROLE_EDITOR no setor jurídico vê apenas chunks permitidos")
    void roleEditorJuridico() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_EDITOR"), "juridico", false, 10);

        assertThat(hits).extracting(LexicalHit::id)
                .containsExactlyInAnyOrder(
                        CHUNK_PUBLICO.toString(),
                        CHUNK_ROLE_EDITOR.toString(),
                        CHUNK_SETOR_JURIDICO.toString())
                .doesNotContain(
                        CHUNK_ROLE_USER.toString(),
                        CHUNK_SETOR_FINANCEIRO.toString());
    }

    @Test
    @DisplayName("admin vê todos os chunks, ignorando role e setor")
    void adminVeTudo() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_ADMIN"), "qualquer", true, 10);

        assertThat(hits).extracting(LexicalHit::id)
                .containsExactlyInAnyOrder(
                        CHUNK_PUBLICO.toString(),
                        CHUNK_ROLE_USER.toString(),
                        CHUNK_ROLE_EDITOR.toString(),
                        CHUNK_SETOR_FINANCEIRO.toString(),
                        CHUNK_SETOR_JURIDICO.toString());
    }

    @Test
    @DisplayName("lista de roles vazia não retorna nada (não vira TRUE acidental)")
    void rolesVaziosNaoVazam() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of(), "financeiro", false, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("setor sem correspondência não retorna nada")
    void setorSemCorrespondencia() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_USER"), "setor-inexistente", false, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("termo inexistente retorna vazio sem erro")
    void termoInexistente() {
        List<LexicalHit> hits = repository.search(
                "xilofone", List.of("ROLE_USER"), "financeiro", false, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("limit é respeitado")
    void limitRespeitado() {
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_ADMIN"), "qualquer", true, 2);

        assertThat(hits).hasSize(2);
    }

    @Test
    @DisplayName("stemming português: 'contratos' no conteúdo casa com 'contrato'")
    void stemmingPortugues() {
        UUID plural = UUID.randomUUID();
        inserir(plural,
                "Os contratos firmados em 2026 seguem o novo modelo.",
                """
                {"allowedRoles": ["ROLE_USER"], "allowedSectors": ["financeiro"]}
                """);

        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_USER"), "financeiro", false, 10);

        assertThat(hits).extracting(LexicalHit::id).contains(plural.toString());
    }

    @Test
    @DisplayName("aspas simples no setor não quebram o SQL")
    void setorComAspaSimples() {
        // quoteLiteral escapa ' como ''. Se não escapasse, o SQL quebraria.
        List<LexicalHit> hits = repository.search(
                "contrato", List.of("ROLE_USER"), "setor'com'aspas", false, 10);

        assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("rank é preenchido e ordenado decrescente")
    void rankOrdenado() {
        List<LexicalHit> hits = repository.search(
                "contrato fornecimento", List.of("ROLE_ADMIN"), "qualquer", true, 10);

        assertThat(hits).isNotEmpty();
        for (int i = 1; i < hits.size(); i++) {
            assertThat(hits.get(i - 1).rank())
                    .isGreaterThanOrEqualTo(hits.get(i).rank());
        }
    }
}