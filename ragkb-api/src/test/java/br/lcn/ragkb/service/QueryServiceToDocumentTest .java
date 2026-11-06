package br.lcn.ragkb.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import br.lcn.ragkb.repository.KbLexicalSearchRepository;
import br.lcn.ragkb.repository.KbLexicalSearchRepository.LexicalHit;

/**
 * Teste do parsing de metadata JSON dos hits lexicais.
 *
 * <p>Este é o ponto onde a citação de fonte quebra silenciosamente: se o
 * metadata vier malformado, o Document sai sem articleId e resolveLabel()
 * retorna "Fonte sem nome" — o usuário vê uma resposta sem fonte, sem erro
 * visível.</p>
 *
 * <p>O método toDocument é private. O teste usa reflexão, o que é aceitável
 * para um método puro sem dependências. Se o método for renomeado, o teste
 * quebra na compilação — falha visível, não silenciosa.</p>
 *
 * <p>Não há dependência de Spring aqui: o QueryService é instanciado com nulls
 * porque toDocument não usa nenhum campo injetado.</p>
 */
class QueryServiceToDocumentTest {

    private QueryService queryService;
    private Method toDocument;

    @BeforeEach
    void setUp() throws Exception {
        // toDocument não usa nenhum campo injetado — nulls são seguros.
        // A ordem segue a declaração dos campos finais no QueryService:
        // vectorStore, chatClient, ticketService, metadataRepository,
        // conversationService, redisChatHistoryService, userService,
        // reminderService, routingService, queryExecutor, integrationRepository,
        // paramService, paramStateService, kbLexicalSearchRepository,
        // reciprocalRankFusion
        queryService = new QueryService(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        toDocument = QueryService.class.getDeclaredMethod(
                "toDocument", KbLexicalSearchRepository.LexicalHit.class);
        toDocument.setAccessible(true);
    }

    private Document invoke(LexicalHit hit) throws Exception {
        return (Document) toDocument.invoke(queryService, hit);
    }

    @Test
    @DisplayName("JSON válido com articleId é preservado no Document")
    void jsonValidoComArticleId() throws Exception {
        String metadata = """
                {"articleId": "art-123", "articleTitle": "Manual de Compras",
                 "articleUrl": "https://kb.local/art-123",
                 "allowedRoles": ["ROLE_USER"], "allowedSectors": ["financeiro"]}
                """;
        LexicalHit hit = new LexicalHit("chunk-1", "conteúdo", metadata, 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-1");
        assertThat(doc.getText()).isEqualTo("conteúdo");
        assertThat(doc.getMetadata())
                .containsEntry("articleId", "art-123")
                .containsEntry("articleTitle", "Manual de Compras")
                .containsEntry("articleUrl", "https://kb.local/art-123");
    }

    @Test
    @DisplayName("JSON válido com documentId e filename é preservado")
    void jsonValidoComDocumentId() throws Exception {
        String metadata = """
                {"documentId": "doc-456", "filename": "politica-compras.pdf",
                 "allowedRoles": ["ROLE_USER"], "allowedSectors": ["financeiro"]}
                """;
        LexicalHit hit = new LexicalHit("chunk-2", "conteúdo", metadata, 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getMetadata())
                .containsEntry("documentId", "doc-456")
                .containsEntry("filename", "politica-compras.pdf");
    }

    @Test
    @DisplayName("JSON válido com lista de roles mantém a lista como List")
    void jsonValidoComLista() throws Exception {
        String metadata = """
                {"allowedRoles": ["ROLE_USER", "ROLE_EDITOR"],
                 "allowedSectors": ["financeiro"]}
                """;
        LexicalHit hit = new LexicalHit("chunk-3", "conteúdo", metadata, 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getMetadata().get("allowedRoles"))
                .isInstanceOf(List.class)
                .asList()
                .containsExactly("ROLE_USER", "ROLE_EDITOR");
    }

    @Test
    @DisplayName("JSON inválido não lança exceção e devolve metadata vazia")
    void jsonInvalido() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-4", "conteúdo", "{isto não é json", 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-4");
        assertThat(doc.getText()).isEqualTo("conteúdo");
        assertThat(doc.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("metadata vazia devolve Document com metadata vazia")
    void metadataVazia() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-5", "conteúdo", "", 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-5");
        assertThat(doc.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("metadata null não lança exceção")
    void metadataNull() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-6", "conteúdo", null, 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-6");
        assertThat(doc.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("JSON 'null' literal devolve metadata vazia")
    void jsonNullLiteral() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-7", "conteúdo", "null", 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-7");
        assertThat(doc.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("JSON array em vez de objeto devolve metadata vazia")
    void jsonArray() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-8", "conteúdo", "[1,2,3]", 0.5);

        Document doc = invoke(hit);

        assertThat(doc.getId()).isEqualTo("chunk-8");
        assertThat(doc.getMetadata()).isEmpty();
    }

    @Test
    @DisplayName("content nulo devolve null em vez de lançar exceção")
    void contentNulo() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-9", null, "{}", 0.5);

        Document doc = invoke(hit);

        // Document.builder().text(null) lança IllegalArgumentException no
        // Spring AI 2.0.1. toDocument intercepta e devolve null; a fusão
        // descarta. Sem isso, um chunk corrompido derrubaria a consulta.
        assertThat(doc).isNull();
    }

    @Test
    @DisplayName("content em branco devolve null em vez de lançar exceção")
    void contentEmBranco() throws Exception {
        LexicalHit hit = new LexicalHit("chunk-10", "   ", "{}", 0.5);

        Document doc = invoke(hit);

        assertThat(doc).isNull();
    }
}