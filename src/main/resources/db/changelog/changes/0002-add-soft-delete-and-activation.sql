-- liquibase formatted sql

-- changeset pmrodrigues:0002-add-deleted-column
ALTER TABLE users ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
-- rollback ALTER TABLE users DROP COLUMN deleted;

-- changeset pmrodrigues:0002-add-activation-token
ALTER TABLE users ADD COLUMN activation_token VARCHAR(255);
ALTER TABLE users ADD COLUMN activation_token_expiry TIMESTAMP;
-- rollback ALTER TABLE users DROP COLUMN activation_token_expiry; ALTER TABLE users DROP COLUMN activation_token;