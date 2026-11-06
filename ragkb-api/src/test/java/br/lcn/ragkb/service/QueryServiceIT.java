package br.lcn.ragkb.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.lcn.ragkb.dto.AnswerResponse;
import br.lcn.ragkb.dto.SourceReferenceDto;
import br.lcn.ragkb.entity.AppRole;
import br.lcn.ragkb.entity.AppUser;
import br.lcn.ragkb.entity.DocumentMetadata;
import br.lcn.ragkb.entity.Sector;
import br.lcn.ragkb.repository.AppUserRepository;
import br.lcn.ragkb.repository.DocumentMetadataRepository;
import br.lcn.ragkb.repository.SectorRepository;
import br.lcn.ragkb.support.IntegrationTestBase;

/**
 * Integração do caminho lexical completo de {@link QueryService#ask}: SQL real
 * no PostgreSQL, fusão RRF e resposta com citação de fonte.
 *
 * <p>Não valida o filtro semântico ({@code filterExpression} do Spring AI):
 * o {@code VectorStore} é dublado para retornar vazio, pois embeddings reais
 * exigiriam chamada de rede ao Google GenAI. Também não valida a qualidade da
 * resposta do LLM, que é dublado com texto fixo que ecoa o prompt e permite
 * inspecionar o contexto recebido.</p>
 *
 * <p>Pré-requisitos: {@code docker compose up -d postgres}, banco
 * {@code ragkb_test} criado e Redis rodando em {@code localhost:6379}.</p>
 */
class QueryServiceIT extends IntegrationTestBase {

    private static final String USERNAME = "usuario.teste";
    private static final String DOC_PERMITIDO = "doc-permitido";
    private static final String DOC_PROIBIDO = "doc-proibido";
    private static final String FILENAME_PERMITIDO = "politica-financeira.pdf";
    private static final String FILENAME_PROIBIDO = "segredo-juridico.pdf";

    @Autowired private QueryService queryService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private DocumentMetadataRepository documentMetadataRepository;

    @MockitoBean private VectorStore vectorStore;
    @MockitoBean private ChatClient chatClient;
    @MockitoBean private IntegrationRoutingService routingService;

    private final AtomicReference<String> capturedPrompt = new AtomicReference<>();
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void prepararFixtures() {
        limparTabelas();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(routingService.isEnabled()).thenReturn(false);

        Sector sector = sectorRepository.save(new Sector("financeiro"));
        appUserRepository.save(new AppUser(USERNAME, "nao-usada", sector,
                List.of(AppRole.ROLE_USER.simpleName()), "Usuário de Teste",
                "usuario.teste@example.com", null));
        authentication = new UsernamePasswordAuthenticationToken(USERNAME, "",
                List.of(new SimpleGrantedAuthority(AppRole.ROLE_USER.authority())));

        inserirDocumento(DOC_PERMITIDO, FILENAME_PERMITIDO,
                List.of("ROLE_USER"), List.of("financeiro"));
        inserirDocumento(DOC_PROIBIDO, FILENAME_PROIBIDO,
                List.of("ROLE_EDITOR"), List.of("juridico"));
        inserirChunk("chunk-permitido", "procedimento financeiro pagamento reembolso aprovado",
                metadata(DOC_PERMITIDO, FILENAME_PERMITIDO, "ROLE_USER", "financeiro"));
        inserirChunk("chunk-proibido", "procedimento financeiro pagamento reembolso confidencial",
                metadata(DOC_PROIBIDO, FILENAME_PROIBIDO, "ROLE_EDITOR", "juridico"));

        configurarChatClient();
    }

    @AfterEach
    void limparFixtures() {
        limparTabelas();
    }

    private void configurarChatClient() {
        ChatClient.ChatClientRequestSpec request = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.PromptUserSpec user = org.mockito.Mockito.mock(ChatClient.PromptUserSpec.class);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<ChatClient.PromptUserSpec> consumer = invocation.getArgument(0);
            consumer.accept(user);
            return request;
        }).when(request).user(any(java.util.function.Consumer.class));
        when(request.system(anyString())).thenReturn(request);
        when(user.text(anyString())).thenAnswer(invocation -> {
            capturedPrompt.set(invocation.getArgument(0));
            return user;
        });
        when(chatClient.prompt()).thenReturn(request);
        ChatClient.CallResponseSpec response = org.mockito.Mockito.mock(ChatClient.CallResponseSpec.class);
        when(request.call()).thenReturn(response);
        when(response.content()).thenAnswer(invocation -> "Resposta baseada em: " + capturedPrompt.get());
    }

    @Test
    @DisplayName("ROLE_USER recebe resposta com citação apenas do documento permitido")
    void usuarioRecebeSomenteFontePermitida() {
        AnswerResponse response = queryService.ask("procedimento financeiro pagamento reembolso", null, authentication);

        assertThat(response.status()).isEqualTo("KNOWLEDGE");
        assertThat(response.answer()).isNotNull().isNotEmpty();
        assertThat(response.sources()).contains(SourceReferenceDto.fromDocument(FILENAME_PERMITIDO));
        assertThat(response.sources()).noneMatch(source -> FILENAME_PROIBIDO.equals(source.label()));
    }

    @Test
    @DisplayName("chunk altamente relevante de outro setor não entra no prompt nem na resposta")
    void chunkProibidoNaoEntraNoContexto() {
        AnswerResponse response = queryService.ask("procedimento financeiro pagamento reembolso", null, authentication);

        assertThat(capturedPrompt.get()).contains(FILENAME_PERMITIDO).doesNotContain(FILENAME_PROIBIDO);
        assertThat(response.answer()).doesNotContain(FILENAME_PROIBIDO);
        assertThat(response.sources()).noneMatch(source -> FILENAME_PROIBIDO.equals(source.label()));
    }

    @Test
    @DisplayName("pergunta sem hits sugere abertura de chamado")
    void semHitsSugereTicket() {
        AnswerResponse response = queryService.ask("zebrafish quasar xilofone", null, authentication);

        assertThat(response.status()).isEqualTo("TICKET_SUGGESTED");
        assertThat(response.ticketSuggestion()).isNotNull();
    }

    @Test
    @DisplayName("metadata JSON malformada não derruba a consulta")
    void metadataMalformadaNaoDerrubaConsulta() {
        // metadata é uma coluna PostgreSQL JSON: ela rejeita sintaxe JSON inválida
        // no INSERT. Um escalar JSON válido, mas incompatível com Map, verifica
        // a degradação do parser sem alterar o schema ou a coluna.
        inserirChunk("chunk-metadata-invalida", "procedimento financeiro pagamento reembolso", "42");

        var adminAuthentication = new UsernamePasswordAuthenticationToken(USERNAME, "",
                List.of(new SimpleGrantedAuthority(AppRole.ROLE_ADMIN.authority())));
        AnswerResponse response = queryService.ask("procedimento financeiro pagamento reembolso", null, adminAuthentication);

        assertThat(response).isNotNull();
        assertThat(response.status()).isIn("KNOWLEDGE", "TICKET_SUGGESTED");
        assertThat(response.answer()).isNotNull().isNotEmpty();
    }

    private void inserirDocumento(String id, String filename, List<String> roles, List<String> sectors) {
        documentMetadataRepository.save(new DocumentMetadata(id, filename, "financeiro",
                UUID.randomUUID().toString().replace("-", ""), 1, null, "texto de origem", roles, sectors));
    }

    private void inserirChunk(String id, String content, String metadataJson) {
        jdbcTemplate.update("INSERT INTO vector_store (id, content, metadata) VALUES (CAST(? AS uuid), ?, CAST(? AS json))",
                idChunk(id), content, metadataJson);
    }

    private static UUID idChunk(String ignored) {
        // UUID determinístico por nome evita dependência de ordenação ou estado entre testes.
        return UUID.nameUUIDFromBytes(ignored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String metadata(String documentId, String filename, String role, String sector) {
        return """
                {"documentId":"%s","filename":"%s","allowedRoles":["%s"],"allowedSectors":["%s"]}
                """.formatted(documentId, filename, role, sector);
    }

    private void limparTabelas() {
        jdbcTemplate.execute("DELETE FROM chat_message_sources");
        jdbcTemplate.execute("DELETE FROM chat_messages");
        jdbcTemplate.execute("DELETE FROM conversations");
        jdbcTemplate.execute("DELETE FROM vector_store");
        jdbcTemplate.execute("DELETE FROM document_allowed_roles");
        jdbcTemplate.execute("DELETE FROM document_allowed_sectors");
        jdbcTemplate.execute("DELETE FROM documents");
        jdbcTemplate.execute("DELETE FROM app_user_roles");
        jdbcTemplate.execute("DELETE FROM app_users");
        jdbcTemplate.execute("DELETE FROM sectors");
    }
}
