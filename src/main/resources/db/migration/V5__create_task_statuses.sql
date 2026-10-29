CREATE TABLE task_statuses (
    code VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL
);

INSERT INTO task_statuses (code, label, display_order)
VALUES
    ('TODO', 'To do', 1),
    ('IN_PROGRESS', 'In progress', 2),
    ('DONE', 'Done', 3);

ALTER TABLE tasks
    DROP CONSTRAINT tasks_status_check;

ALTER TABLE tasks
    ADD CONSTRAINT tasks_status_fkey
    FOREIGN KEY (status)
    REFERENCES task_statuses(code);
    