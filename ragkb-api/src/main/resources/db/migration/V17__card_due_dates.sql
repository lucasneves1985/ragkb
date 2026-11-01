-- Prazo (due date) dos cards + controle de idempotência do lembrete.
-- due_date é DATE (sem hora): prazo é um dia, não um instante — o lembrete
-- é disparado pela janela diária do CardDueReminderJob.
-- due_reminder_sent_at marca que o lembrete DESTE prazo já foi enviado;
-- editar o prazo volta a campo a NULL (rearma o lembrete).

ALTER TABLE cards ADD COLUMN due_date DATE;
ALTER TABLE cards ADD COLUMN due_reminder_sent_at TIMESTAMPTZ;

-- Índice parcial: a query do scheduler só toca cards com prazo.
CREATE INDEX idx_cards_due_reminder
    ON cards (due_date)
    WHERE due_date IS NOT NULL;