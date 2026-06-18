-- Turn tracking for payment reminders
ALTER TABLE user_group_memberships
    ADD COLUMN IF NOT EXISTS turn_assigned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reminder_level INTEGER DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_membership_turn_assigned ON user_group_memberships(turn_assigned_at);

-- Invitation table (may already exist via Hibernate in some environments)
CREATE TABLE IF NOT EXISTS invitation_user_to_group_information (
    id BIGSERIAL PRIMARY KEY,
    user_who_sent_invitation BIGINT,
    group_name VARCHAR(255),
    username VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP,
    used_at TIMESTAMP,
    expired_date TIMESTAMP,
    invitation_status VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_invitation_username ON invitation_user_to_group_information(username);
CREATE INDEX IF NOT EXISTS idx_invitation_email ON invitation_user_to_group_information(email);
CREATE INDEX IF NOT EXISTS idx_invitation_status ON invitation_user_to_group_information(invitation_status);