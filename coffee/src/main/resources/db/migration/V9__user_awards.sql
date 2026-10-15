CREATE TABLE IF NOT EXISTS user_awards (
    id BIGSERIAL PRIMARY KEY,
    utente_id BIGINT NOT NULL REFERENCES utenti (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    level VARCHAR(16) NOT NULL,
    icon VARCHAR(64) NOT NULL,
    occurrence_key VARCHAR(160) NOT NULL,
    group_name VARCHAR(128),
    earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_award_occurrence UNIQUE (utente_id, code, occurrence_key)
);

CREATE INDEX IF NOT EXISTS idx_user_awards_utente_earned
    ON user_awards (utente_id, earned_at DESC);
