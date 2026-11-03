-- Cards: Kanban pessoal (board por projeto, colunas = status).
-- Status é persistido no card por conveniência de query; a coluna do
-- board é a fonte da verdade. Toda transição é logada em
-- card_status_events (respostas temporais do assistente).

CREATE TABLE projects (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    color       VARCHAR(7),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_projects_name UNIQUE (name)
);

CREATE TABLE cards (
    id          VARCHAR(36) PRIMARY KEY,
    project_id  VARCHAR(36)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(30)  NOT NULL DEFAULT 'PENDENTE',
    position    INTEGER      NOT NULL DEFAULT 0,
    closed_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cards_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT ck_cards_status CHECK (status IN
        ('PENDENTE', 'EM_ANDAMENTO', 'AGUARDANDO_FORNECEDOR',
         'AGUARDANDO_RETORNO', 'CONCLUIDO', 'CANCELADO'))
);
CREATE INDEX idx_cards_project_status ON cards (project_id, status, position);

CREATE TABLE card_comments (
    id         VARCHAR(36) PRIMARY KEY,
    card_id    VARCHAR(36) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_comments_card FOREIGN KEY (card_id)
        REFERENCES cards (id) ON DELETE CASCADE
);
CREATE INDEX idx_comments_card ON card_comments (card_id, created_at);

CREATE TABLE card_status_events (
    id          VARCHAR(36) PRIMARY KEY,
    card_id     VARCHAR(36) NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_status_events_card FOREIGN KEY (card_id)
        REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT ck_status_events_from CHECK (from_status IS NULL OR from_status IN
        ('PENDENTE', 'EM_ANDAMENTO', 'AGUARDANDO_FORNECEDOR',
         'AGUARDANDO_RETORNO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_status_events_to CHECK (to_status IN
        ('PENDENTE', 'EM_ANDAMENTO', 'AGUARDANDO_FORNECEDOR',
         'AGUARDANDO_RETORNO', 'CONCLUIDO', 'CANCELADO'))
);
CREATE INDEX idx_status_events_card ON card_status_events (card_id, changed_at);