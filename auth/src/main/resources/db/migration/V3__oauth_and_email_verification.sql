ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(50) DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expired_date TIMESTAMP NOT NULL,
    token_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_tokens(token);