package br.lcn.ragkb.service;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acesso nativo (JDBC) à coluna pgvector description_embedding das integrações.
 * Fora do mapeamento JPA de propósito: Hibernate não mapeia o tipo vector sem
 * converter customizado, e o gate precisa de busca por distância coseno nativa.
 *
 * Mudança de calibração: a busca retorna o TOP-N SEM filtro de threshold — o
 * filtro fica no IntegrationRoutingService, que loga o melhor score mesmo
 * quando rejeita. Sem isso, um piso mal calibrado é invisível ("Sem candidatos"
 * não diz quanto a pergunta tirou).
 */
@Repository
public class IntegrationEmbeddingStore {

    /**
     * Top-N integrações QUERY ativas por similaridade, SEM filtro de piso.
     */
    private static final String FIND_TOP_CANDIDATES = """
            SELECT i.id,
                   i.name,
                   i.context_description,
                   1 - (i.description_embedding <=> cast(:vec as vector)) AS similarity
            FROM integrations i
            WHERE i.integration_type = 'QUERY'
              AND i.active = true
              AND i.description_embedding IS NOT NULL
            ORDER BY similarity DESC
            LIMIT :limit
            """;

    private static final String UPDATE_EMBEDDING
            = "UPDATE integrations SET description_embedding = cast(:vec as vector) WHERE id = :id";

    private static final String CLEAR_EMBEDDING
            = "UPDATE integrations SET description_embedding = NULL WHERE id = :id";

    private final NamedParameterJdbcTemplate jdbc;

    public IntegrationEmbeddingStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record CandidateMatch(String id, String name, String contextDescription, double similarity) {

    }

    public List<CandidateMatch> findTop(float[] questionEmbedding, int limit) {
        var params = new MapSqlParameterSource()
                .addValue("vec", toPgVector(questionEmbedding))
                .addValue("limit", limit);
        return jdbc.query(FIND_TOP_CANDIDATES, params, (rs, rowNum) -> new CandidateMatch(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("context_description"),
                rs.getDouble("similarity")));
    }

    public void updateEmbedding(String integrationId, float[] embedding) {
        jdbc.update(UPDATE_EMBEDDING, new MapSqlParameterSource()
                .addValue("vec", toPgVector(embedding))
                .addValue("id", integrationId));
    }

    public void clearEmbedding(String integrationId) {
        jdbc.update(CLEAR_EMBEDDING, new MapSqlParameterSource().addValue("id", integrationId));
    }

    /**
     * pgvector aceita literal '[0.1,0.2,...]' com cast explícito. Loop em vez
     * de Arrays.stream: não existe overload para float[] (apenas int[], long[],
     * double[] e T[]) — FloatStream não existe em Java 21.
     */
    private String toPgVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding vazio ou nulo.");
        }
        StringBuilder sb = new StringBuilder(embedding.length * 12).append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }
}
