
CREATE TABLE cases (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT
);

ALTER TABLE tasks
ADD COLUMN case_id BIGINT NOT NULL
    REFERENCES cases(id)
    ON DELETE RESTRICT;

INSERT INTO cases (title, description)
VALUES
    ('Missing documents', 'Review outstanding documents and supporting evidence.'),
    ('Benefits appeal', 'Prepare the case for an upcoming appeal hearing.'),
    ('Civil claim', 'Review correspondence and prepare case documentation.');
