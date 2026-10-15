-- Group rules
ALTER TABLE user_group
    ADD COLUMN IF NOT EXISTS max_skip_per_round INTEGER,
    ADD COLUMN IF NOT EXISTS pay_for_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS pay_for_admin_only BOOLEAN DEFAULT FALSE;

-- Payment beneficiary (paga-per tracking)
ALTER TABLE pagamento
    ADD COLUMN IF NOT EXISTS beneficiary_username VARCHAR(255);

-- User payment links
ALTER TABLE utenti
    ADD COLUMN IF NOT EXISTS satispay_link VARCHAR(500),
    ADD COLUMN IF NOT EXISTS revolut_link VARCHAR(500);

-- Gamification stats per membership
ALTER TABLE user_group_memberships
    ADD COLUMN IF NOT EXISTS payment_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS skip_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_streak INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS round_skip_count INTEGER DEFAULT 0;