-- Extend the type constraint installed by Hibernate in pre-podcast databases.
ALTER TABLE IF EXISTS contents DROP CONSTRAINT IF EXISTS contents_type_check;
ALTER TABLE IF EXISTS contents ADD CONSTRAINT contents_type_check
    CHECK (type IN ('ARTICLE', 'VIDEO', 'PODCAST', 'AUDIO', 'IMAGE'));
