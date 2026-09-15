-- Articles: user-authored knowledge base content (rich text HTML).
-- Images are referenced by URL only (never base64); only the text
-- content is ingested into the vector store.

CREATE TABLE articles (
    id                  VARCHAR(36)   PRIMARY KEY,
    title               VARCHAR(200)  NOT NULL,
    content             TEXT          NOT NULL,
    sector              VARCHAR(80)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    author_username     VARCHAR(100)  NOT NULL,
    chunk_count         INTEGER       NOT NULL DEFAULT 0,
    version             INTEGER       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    published_at        TIMESTAMPTZ
);

CREATE TABLE article_allowed_sectors (
    article_id  VARCHAR(36) NOT NULL,
    sector_name VARCHAR(80) NOT NULL,
    CONSTRAINT fk_article_allowed_sectors_article
        FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    CONSTRAINT uq_article_sector UNIQUE (article_id, sector_name)
);

CREATE INDEX idx_articles_status        ON articles (status);
CREATE INDEX idx_articles_author        ON articles (author_username);
CREATE INDEX idx_articles_sector        ON articles (sector);
CREATE INDEX idx_article_sectors_sector ON article_allowed_sectors (sector_name);