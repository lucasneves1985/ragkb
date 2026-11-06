-- ============================================================
-- V20 - Compartilhamento de projetos (owner + participantes)
-- ============================================================
-- Modelo de permissão (decisões do autor):
-- - O criador do projeto é o OWNER: gerencia o projeto e o
--   compartilhamento, e participa do board como qualquer analista.
-- - Participante (project_shares): controle total sobre os CARDS do
--   projeto, EXCETO exclusão — delete permanece exclusivo do criador
--   do card.
-- - A lista de projetos passa a mostrar apenas owner + participantes.
-- - Projetos legados (created_by NULL, anteriores a esta migration)
--   permanecem visíveis a todos, com privacidade de cards por criador
--   preservada — não há dados de produção e não há backfill.
-- - Digest e assistente de cards passam a cobrir cards próprios +
--   cards de projetos onde o usuário é participante (marco 2).
-- - Lembretes de prazo continuam roteados ao criador do card.

ALTER TABLE projects ADD COLUMN created_by BIGINT;

ALTER TABLE projects
ADD CONSTRAINT fk_projects_owner
    FOREIGN KEY (created_by) REFERENCES app_users (id);

CREATE TABLE project_shares (
    id         VARCHAR(36) PRIMARY KEY,
    project_id VARCHAR(36) NOT NULL,
    user_id    BIGINT      NOT NULL,
    shared_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_shares UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_shares_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_shares_user
        FOREIGN KEY (user_id) REFERENCES app_users (id) ON DELETE CASCADE
);

CREATE INDEX idx_project_shares_user ON project_shares (user_id);