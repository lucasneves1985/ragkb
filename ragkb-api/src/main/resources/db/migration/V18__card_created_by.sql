-- Autoria do card: quem criou, para rotear o lembrete de prazo ao
-- telefone do cadastro do próprio criador (AppUser.phone).
-- Nullable: cards criados antes desta migration não têm dono conhecido —
-- o CardDueReminderJob os pula com warning em vez de falhar.

ALTER TABLE cards ADD COLUMN created_by BIGINT;

ALTER TABLE cards
    ADD CONSTRAINT fk_cards_creator
    FOREIGN KEY (created_by) REFERENCES app_users (id);

CREATE INDEX idx_cards_creator ON cards (created_by);