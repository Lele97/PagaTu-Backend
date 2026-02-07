-- =====================================================
-- PagaTu Auth Service - Initial Schema Migration
-- Version: V1
-- Description: Creates users and token_password_reset tables
-- =====================================================

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    birthdate DATE NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    user_groups TEXT
);

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create token_password_reset table
CREATE TABLE IF NOT EXISTS token_password_reset (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255),
    email VARCHAR(255),
    expired_date TIMESTAMP,
    token_status VARCHAR(50),
    created_at TIMESTAMP,
    used_at TIMESTAMP
);

-- Create indexes for token_password_reset table
CREATE INDEX IF NOT EXISTS idx_token_password_reset_token ON token_password_reset(token);
CREATE INDEX IF NOT EXISTS idx_token_password_reset_email ON token_password_reset(email);
CREATE INDEX IF NOT EXISTS idx_token_password_reset_status ON token_password_reset(token_status);
