-- liquibase formatted sql
-- changeset pmrodrigues:0035-drop-dtype-from-users dbms:postgresql

-- Drop the dtype discriminator column from users.
-- The JOINED inheritance between User and Pessoa no longer uses a discriminator column;
-- Hibernate determines the concrete type via table-presence (LEFT JOIN to pessoas).
-- The SINGLE_TABLE discriminator within pessoas (pessoa_tipo) is unaffected.
ALTER TABLE users DROP COLUMN IF EXISTS dtype;

-- rollback ALTER TABLE users ADD COLUMN dtype VARCHAR(31) NOT NULL DEFAULT 'USER';
