
-- Add case ownership, initially nullable to accommodate existing cases.
ALTER TABLE cases
ADD COLUMN owner_id BIGINT
    REFERENCES caseworkers(id)
    ON DELETE RESTRICT;

-- Assign the three existing cases to demonstration caseworkers.
UPDATE cases
SET owner_id = (
    SELECT id FROM caseworkers WHERE name = 'Danger Mouse'
)
WHERE title = 'Missing documents';

UPDATE cases
SET owner_id = (
    SELECT id FROM caseworkers WHERE name = 'Count Duckula'
)
WHERE title = 'Benefits appeal';

UPDATE cases
SET owner_id = (
    SELECT id FROM caseworkers WHERE name = 'Super Ted'
)
WHERE title = 'Civil claim';

-- Seed two additional cases for each caseworker.
INSERT INTO cases (title, description, owner_id)
VALUES
    (
        'Evidence review',
        'Review submitted evidence and supporting documentation.',
        (SELECT id FROM caseworkers WHERE name = 'Danger Mouse')
    ),
    (
        'Hearing preparation',
        'Prepare documentation for the upcoming hearing.',
        (SELECT id FROM caseworkers WHERE name = 'Danger Mouse')
    ),
    (
        'Witness statements',
        'Review witness statements and identify outstanding information.',
        (SELECT id FROM caseworkers WHERE name = 'Count Duckula')
    ),
    (
        'Case correspondence',
        'Review outstanding correspondence relating to the case.',
        (SELECT id FROM caseworkers WHERE name = 'Count Duckula')
    ),
    (
        'Application review',
        'Review the submitted application and supporting materials.',
        (SELECT id FROM caseworkers WHERE name = 'Super Ted')
    ),
    (
        'Decision preparation',
        'Prepare documentation and recommendations for the case decision.',
        (SELECT id FROM caseworkers WHERE name = 'Super Ted')
    );

-- Make case ownership mandatory once all cases have an owner.
ALTER TABLE cases
ALTER COLUMN owner_id SET NOT NULL;

-- Index case ownership to support filtering by caseworker.
CREATE INDEX idx_cases_owner_id ON cases(owner_id);
