ALTER TABLE integration_executions
    ADD COLUMN action_status VARCHAR(20),
    ADD COLUMN action_error TEXT;
