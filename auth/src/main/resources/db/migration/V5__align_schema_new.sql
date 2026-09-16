ALTER TABLE email_verification_tokens
    ADD COLUMN IF NOT EXISTS email VARCHAR(255);

ALTER TABLE email_verification_tokens
    ALTER COLUMN email SET NOT NULL;