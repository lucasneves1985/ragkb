-- ============================================================
-- V19 - Busca híbrida na KB: lado lexical (FTS) sobre a vector_store
-- ============================================================
-- A KB busca em semântica pura (vector_store + HNSW cosine). Números de
-- processo, nomes próprios, siglas e códigos de lei ficam mal representados
-- em embeddings — o lado lexical (FTS nativo do Postgres) cobre esses casos.
--
-- Coluna GERADA STORED: o Postgres recalcula a cada INSERT/UPDATE de content
-- e materializa a conversão em lote para todos os chunks existentes no
-- próprio ALTER — não há backfill manual.
--
-- to_tsvector na forma de 2 argumentos ('portuguese', content) é
-- OBRIGATÓRIA: a forma de 1 argumento depende do GUC default_text_search_config
-- (STABLE) e o Postgres rejeita em coluna gerada. A config 'portuguese' traz
-- stemming e stopwords corretos para o domínio e preserva números/nomes na
-- tokenização.
--
-- Chunks são write-once (ingestão idempotente por hash); o custo de manter a
-- tsvector é de escrita e não afeta o caminho de leitura semântica.

ALTER TABLE vector_store
    ADD COLUMN content_tsv tsvector
    GENERATED ALWAYS AS (to_tsvector('portuguese', content)) STORED;

CREATE INDEX idx_vector_store_content_tsv
    ON vector_store USING gin (content_tsv);