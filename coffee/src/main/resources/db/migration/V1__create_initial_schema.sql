-- =====================================================
-- PagaTu Coffee Service - Initial Schema Migration
-- Version: V1
-- Description: Creates utenti, user_group, user_group_memberships, and pagamento tables
-- =====================================================

-- Create utenti table
CREATE TABLE IF NOT EXISTS utenti (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    auth_id BIGINT NOT NULL,
    name VARCHAR(255),
    lastname VARCHAR(255)
);

-- Create indexes for utenti table
CREATE INDEX IF NOT EXISTS idx_utenti_username ON utenti(username);
CREATE INDEX IF NOT EXISTS idx_utenti_email ON utenti(email);
CREATE INDEX IF NOT EXISTS idx_utenti_auth_id ON utenti(auth_id);

-- Create user_group table
CREATE TABLE IF NOT EXISTS user_group (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- Create index for user_group table
CREATE INDEX IF NOT EXISTS idx_user_group_name ON user_group(name);

-- Create user_group_memberships table
CREATE TABLE IF NOT EXISTS user_group_memberships (
    id BIGSERIAL PRIMARY KEY,
    utente_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    my_turn BOOLEAN,
    joined_at TIMESTAMP,
    is_admin BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_membership_utente FOREIGN KEY (utente_id) REFERENCES utenti(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_group FOREIGN KEY (group_id) REFERENCES user_group(id) ON DELETE CASCADE,
    CONSTRAINT uk_utente_group UNIQUE (utente_id, group_id)
);

-- Create indexes for user_group_memberships table
CREATE INDEX IF NOT EXISTS idx_membership_utente ON user_group_memberships(utente_id);
CREATE INDEX IF NOT EXISTS idx_membership_group ON user_group_memberships(group_id);
CREATE INDEX IF NOT EXISTS idx_membership_status ON user_group_memberships(status);
CREATE INDEX IF NOT EXISTS idx_membership_my_turn ON user_group_memberships(my_turn);

-- Create pagamento table
CREATE TABLE IF NOT EXISTS pagamento (
    id BIGSERIAL PRIMARY KEY,
    user_group_membership BIGINT NOT NULL,
    importo DOUBLE PRECISION,
    descrizione VARCHAR(255),
    data_pagamento TIMESTAMP,
    CONSTRAINT fk_pagamento_membership FOREIGN KEY (user_group_membership) REFERENCES user_group_memberships(id) ON DELETE CASCADE
);

-- Create indexes for pagamento table
CREATE INDEX IF NOT EXISTS idx_pagamento_membership ON pagamento(user_group_membership);
CREATE INDEX IF NOT EXISTS idx_pagamento_data ON pagamento(data_pagamento);
