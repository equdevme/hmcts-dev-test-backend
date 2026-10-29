ALTER TABLE task_statuses
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE task_statuses
SET is_default = TRUE
WHERE code = 'TODO';

CREATE UNIQUE INDEX task_statuses_one_default_idx
    ON task_statuses (is_default)
    WHERE is_default = TRUE;
    