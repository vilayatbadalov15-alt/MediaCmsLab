-- Earlier Hibernate installations may already contain the media fields and episode table.
ALTER TABLE contents
    ADD COLUMN IF NOT EXISTS media_url text,
    ADD COLUMN IF NOT EXISTS duration_seconds integer,
    ADD COLUMN IF NOT EXISTS published_at timestamptz,
    ADD COLUMN revision bigint NOT NULL DEFAULT 0;
ALTER TABLE contents DROP CONSTRAINT IF EXISTS contents_type_check;
ALTER TABLE contents ADD CONSTRAINT contents_type_check
    CHECK (type IN ('ARTICLE','VIDEO','AUDIO','IMAGE','PODCAST'));
CREATE TABLE IF NOT EXISTS podcast_episodes (
    content_id bigint NOT NULL REFERENCES contents,
    episode_position integer NOT NULL,
    title varchar(255) NOT NULL,
    PRIMARY KEY (content_id, episode_position)
);
