-- Anexos de cards: mesmo storage físico dos anexos de regras
-- (ragkb.media.attachments-path); nomes UUID, sem colisão.

CREATE TABLE card_attachments (
    id           VARCHAR(36)  PRIMARY KEY,
    card_id      VARCHAR(36)  NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_card_attachments_card FOREIGN KEY (card_id)
        REFERENCES cards (id) ON DELETE CASCADE
);
CREATE INDEX idx_card_attachments_card ON card_attachments (card_id);

-- Chunks vetoriais em tabela PRÓPRIA (mesma decisão do V6 para
-- business_rules): cards NÃO entram no vector_store do assistente geral.
-- Status e datas NÃO são duplicados aqui — entram via join com cards
-- na consulta, evitando drift de denormalização.

CREATE TABLE card_chunks (
    id               VARCHAR(36) PRIMARY KEY,
    card_id          VARCHAR(36) NOT NULL,
    project_id       VARCHAR(36) NOT NULL,
    chunk_type       VARCHAR(10) NOT NULL,
    source_id        VARCHAR(36),
    chunk_index      INTEGER     NOT NULL DEFAULT 0,
    content          TEXT        NOT NULL,
    embedding        vector(1536),
    embedding_status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_card_chunks_card FOREIGN KEY (card_id)
        REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT ck_card_chunks_type CHECK (chunk_type IN ('CARD', 'COMMENT')),
    CONSTRAINT ck_card_chunks_embedding_status
        CHECK (embedding_status IN ('PENDING', 'DONE', 'FAILED'))
);
CREATE INDEX idx_card_chunks_card ON card_chunks (card_id);
CREATE INDEX idx_card_chunks_project ON card_chunks (project_id);
CREATE INDEX idx_card_chunks_retry ON card_chunks (embedding_status);
CREATE INDEX idx_card_chunks_embedding
    ON card_chunks USING hnsw (embedding vector_cosine_ops);