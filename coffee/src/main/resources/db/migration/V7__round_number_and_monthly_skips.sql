ALTER TABLE user_group
    ADD COLUMN IF NOT EXISTS current_round_number INTEGER NOT NULL DEFAULT 1;

ALTER TABLE user_group_memberships
    ADD COLUMN IF NOT EXISTS monthly_skip_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE user_group_memberships
    ADD COLUMN IF NOT EXISTS monthly_skip_period VARCHAR(7);