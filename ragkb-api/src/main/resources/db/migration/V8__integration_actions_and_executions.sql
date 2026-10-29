-- ============================================================
-- V8 - Ações das integrações + histórico de execuções
-- ============================================================

-- Ação executada após a chamada da integração agendada
ALTER TABLE integrations
    ADD COLUMN action_type    VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    ADD COLUMN action_target  VARCHAR(200),
    ADD COLUMN action_template TEXT,
    ADD CONSTRAINT ck_integrations_action
        CHECK (action_type IN ('NONE', 'EMAIL', 'WHATSAPP'));

-- Histórico de execuções (auditoria e diagnóstico do scheduler)
CREATE TABLE integration_executions (
    id             VARCHAR(36) PRIMARY KEY,
    integration_id VARCHAR(36) NOT NULL,
    status         VARCHAR(20) NOT NULL,
    started_at     TIMESTAMPTZ NOT NULL,
    finished_at    TIMESTAMPTZ,
    attempt        INTEGER     NOT NULL DEFAULT 1,
    http_status    INTEGER,
    response_body  TEXT,
    error_message  TEXT,
    CONSTRAINT fk_integration_executions_integration
        FOREIGN KEY (integration_id) REFERENCES integrations (id) ON DELETE CASCADE,
    CONSTRAINT ck_integration_executions_status
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_integration_executions_integration
    ON integration_executions (integration_id, started_at DESC);