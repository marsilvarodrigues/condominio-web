-- liquibase formatted sql
-- changeset pmrodrigues:0034-drop-pessoas-nome dbms:postgresql

-- Migrate nome from pessoas to users.name for records where users.name is blank,
-- then drop the now-redundant column.
UPDATE users
SET name = p.nome
FROM pessoas p
WHERE users.id = p.id
  AND (users.name IS NULL OR users.name = '');

ALTER TABLE pessoas DROP COLUMN IF EXISTS nome;
