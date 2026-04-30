CREATE TABLE notes (
    id         UUID         PRIMARY KEY,
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_notes_created_at ON notes (created_at DESC);
