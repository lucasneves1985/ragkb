-- ============================================================
-- V7 - Configurações da aplicação (key/value) + Integrações externas
-- ============================================================

-- ------------------------------------------------------------
-- AppConfiguration: configurações runtime (WAHA, etc.)
-- Chaves conhecidas: waha.base_url, waha.api_key, waha.session
-- ------------------------------------------------------------
CREATE TABLE app_configuration (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(100)
);

-- ------------------------------------------------------------
-- Integrations: integrações externas (SCHEDULED = pull/agendada,
-- QUERY = consulta roteada pelo LLM na frente 5)
-- ------------------------------------------------------------
CREATE TABLE integrations (
    id                        VARCHAR(36) PRIMARY KEY,
    name                      VARCHAR(120) NOT NULL,
    description               TEXT,
    url                       VARCHAR(500) NOT NULL,
    auth_type                 VARCHAR(20)  NOT NULL DEFAULT 'NONE',
    credentials_encrypted     TEXT,
    request_template          TEXT,
    output_schema             TEXT,
    integration_type          VARCHAR(20)  NOT NULL,
    schedule_cron             VARCHAR(100),
    schedule_timezone         VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    schedule_interval_seconds BIGINT,
    context_description       TEXT,
    params_definition         TEXT,
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by                VARCHAR(100),
    version                   INTEGER      NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_integrations_name UNIQUE (name),
    CONSTRAINT ck_integrations_type
        CHECK (integration_type IN ('SCHEDULED', 'QUERY')),
    CONSTRAINT ck_integrations_auth
        CHECK (auth_type IN ('NONE', 'BEARER', 'BASIC', 'HEADER_CUSTOM')),
    CONSTRAINT ck_integrations_schedule_present
        CHECK (integration_type <> 'SCHEDULED'
               OR schedule_cron IS NOT NULL
               OR schedule_interval_seconds IS NOT NULL),
    CONSTRAINT ck_integrations_context_present
        CHECK (integration_type <> 'QUERY' OR context_description IS NOT NULL)
);