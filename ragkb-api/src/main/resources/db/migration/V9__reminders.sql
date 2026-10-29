-- ============================================================
-- V9 - Lembretes via WhatsApp (frente 4)
-- ============================================================

CREATE TABLE reminders (
    id               VARCHAR(36) PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    conversation_id  VARCHAR(255),
    original_request TEXT        NOT NULL,
    summary          TEXT,
    remind_at        TIMESTAMPTZ NOT NULL,
    sent_at          TIMESTAMPTZ,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count      INTEGER     NOT NULL DEFAULT 0,
    last_error       TEXT,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT ck_reminders_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_reminders_pending ON reminders (status, remind_at);