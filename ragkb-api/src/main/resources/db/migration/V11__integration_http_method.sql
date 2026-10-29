-- ============================================================
-- V11 - Método HTTP configurável (frente 5, fase 3: GET com
-- path/query params via placeholders na URL)
-- ============================================================
-- Default POST preserva o comportamento das integrações existentes.
ALTER TABLE integrations
    ADD COLUMN http_method VARCHAR(10) NOT NULL DEFAULT 'POST';