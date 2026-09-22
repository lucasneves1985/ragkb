-- ============================================================
-- V10 - Embedding da descrição das integrações QUERY (gate de roteamento)
-- ============================================================
-- Dimensão 1536 = gemini-embedding-001 (mesmo modelo do fluxo KB).
-- Vetores gerados/gravados pela aplicação (IntegrationEmbeddingStore),
-- não por JPA — coluna nativa pgvector fora do mapeamento da entidade.
ALTER TABLE integrations
    ADD COLUMN description_embedding vector(1536);