-- Business rules: navigable rules with semantic search on listing.
-- Embedding lives in THIS table (not vector_store): rules are never
-- ingested into the assistant's RAG corpus by design.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE business_rules (
    id               VARCHAR(36)  PRIMARY KEY,
    title            VARCHAR(120) NOT NULL,
    description      TEXT         NOT NULL,
    requester        VARCHAR(120),
    reason           VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    embedding        vector(1536),
    embedding_status VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    author_username  VARCHAR(100) NOT NULL,
    updated_username VARCHAR(100),
    version          INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published_at     TIMESTAMPTZ,
    CONSTRAINT uq_business_rules_title UNIQUE (title)
);

CREATE TABLE business_rule_sectors (
    rule_id     VARCHAR(36) NOT NULL,
    sector_name VARCHAR(80) NOT NULL,
    CONSTRAINT fk_rule_sectors_rule
        FOREIGN KEY (rule_id) REFERENCES business_rules (id) ON DELETE CASCADE,
    CONSTRAINT uq_rule_sector UNIQUE (rule_id, sector_name)
);

CREATE TABLE business_rule_articles (
    rule_id    VARCHAR(36) NOT NULL,
    article_id VARCHAR(36) NOT NULL,
    CONSTRAINT fk_rule_articles_rule
        FOREIGN KEY (rule_id) REFERENCES business_rules (id) ON DELETE CASCADE,
    CONSTRAINT fk_rule_articles_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT uq_rule_article UNIQUE (rule_id, article_id)
);

CREATE TABLE business_rule_attachments (
    id           VARCHAR(36)  PRIMARY KEY,
    rule_id      VARCHAR(36)  NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT       NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_by  VARCHAR(100) NOT NULL,
    uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attachments_rule
        FOREIGN KEY (rule_id) REFERENCES business_rules (id) ON DELETE CASCADE
);

CREATE INDEX idx_rules_status          ON business_rules (status);
CREATE INDEX idx_rules_author          ON business_rules (author_username);
CREATE INDEX idx_rule_sectors_sector   ON business_rule_sectors (sector_name);
CREATE INDEX idx_rule_articles_article ON business_rule_articles (article_id);
CREATE INDEX idx_rules_embedding
    ON business_rules USING hnsw (embedding vector_cosine_ops);