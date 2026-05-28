-- liquibase formatted sql

-- changeset pmrodrigues:0003-add-audit-timestamps
ALTER TABLE users ADD COLUMN created_at TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;
-- rollback ALTER TABLE users DROP COLUMN updated_at; ALTER TABLE users DROP COLUMN created_at;