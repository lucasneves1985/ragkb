package br.lcn.ragkb.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lado lexical (FTS) da busca híbrida da KB — busca tsvector sobre o content da
 * vector_store, com o MESMO filtro de permissões do caminho semântico
 * (QueryService): admin sem filtro; não-admin exige allowedRoles contendo pelo
 * menos um dos papéis do usuário E allowedSectors contendo o setor. Princípio
 * de PARIDADE: a busca lexical nunca revela o que a semântica não revelaria —
 * chunks sem allowedRoles/allowedSectors (ou com valores não casados) ficam
 * invisíveis para não-admin em ambos os caminhos.
 *
 * POR QUE jsonb_exists / jsonb_exists_any (e não os operadores ? e ?|): o
 * caractere '?' colide com o placeholder de parâmetro do JDBC — qualquer
 * simplificação de volta aos operadores quebra a query em runtime. As formas
 * funcionais são equivalentes aos operadores.
 *
 * websearch_to_tsquery: tolera linguagem natural do usuário (mesma entrada do
 * caminho semântico) e trata frases entre aspas como phrase query. Query toda
 * de stopwords retorna tsquery vazio, que não casa com nada — a fusão degrada
 * para semântica pura, sem erro.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class KbLexicalSearchRepository {

    /**
     * Projeção da busca lexical: id do chunk da vector_store, content, metadata
     * cru (JSON em texto) e rank do FTS. O QueryService converte para Document
     * na fusão — mesmo contrato de hidratação do caminho semântico.
     */
    public record LexicalHit(String id, String content, String metadata, double rank) {

    }

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<LexicalHit> ROW_MAPPER = (rs, rowNum) -> new LexicalHit(
            rs.getString("id"),
            rs.getString("content"),
            rs.getString("metadata"),
            rs.getDouble("rank"));

    /**
     * Busca lexical com filtro de permissões. Parâmetros ligados por
     * placeholder (pergunta e limit); os valores de roles/sector entram na SQL
     * apenas via literais escapados — são papéis fixos do sistema (AppRole) e
     * setor do cadastro, nunca texto livre do usuário.
     */
    public List<LexicalHit> search(String question, List<String> roles,
            String userSector, boolean isAdmin, int limit) {
        String permissionFilter = isAdmin
                ? "TRUE"
                : "(" + rolesFilter(roles) + " AND "
                + "jsonb_exists(vs.metadata::jsonb->'allowedSectors', "
                + quoteLiteral(userSector) + "))";
        String sql = """
                WITH q AS (SELECT websearch_to_tsquery('portuguese', CAST(? AS text)) AS tsq)
                SELECT vs.id,
                       vs.content,
                       vs.metadata::text AS metadata,
                       ts_rank_cd(vs.content_tsv, q.tsq) AS rank
                FROM vector_store vs, q
                WHERE vs.content_tsv @@ q.tsq
                  AND %s
                ORDER BY rank DESC, vs.id
                LIMIT ?
                """.formatted(permissionFilter);
        return jdbcTemplate.query(sql, ROW_MAPPER, question, limit);
    }

    private String rolesFilter(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "FALSE";
        }
        String array = roles.stream()
                .map(this::quoteLiteral)
                .collect(Collectors.joining(", ", "ARRAY[", "]::text[]"));
        return "jsonb_exists_any(vs.metadata::jsonb->'allowedRoles', " + array + ")";
    }

    private String quoteLiteral(String value) {
        return "'" + (value == null ? "" : value.replace("'", "''")) + "'";
    }
}
