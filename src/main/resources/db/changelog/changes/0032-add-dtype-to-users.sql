-- liquibase formatted sql
-- changeset pmrodrigues:0032-add-dtype-to-users dbms:postgresql
-- Add JPA JOINED inheritance discriminator column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS dtype VARCHAR(31) NOT NULL DEFAULT 'USER';
CREATE INDEX IF NOT EXISTS idx_users_dtype ON users (dtype);
-- rollback ALTER TABLE users DROP COLUMN IF EXISTS dtype;
