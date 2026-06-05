-- liquibase formatted sql
-- changeset pmrodrigues:0033-rename-pessoa-fisica-to-morador dbms:postgresql

-- Rename discriminator PF → MORADOR for the Morador entity
UPDATE pessoas SET pessoa_tipo = 'MORADOR' WHERE pessoa_tipo = 'PF';

-- Soft-delete user accounts that were PessoaJuridica (PJ concept removed)
UPDATE users SET deleted = true
WHERE id IN (SELECT id FROM pessoas WHERE pessoa_tipo = 'PJ');

-- Remove PJ pessoa records
DELETE FROM pessoas WHERE pessoa_tipo = 'PJ';

-- rollback UPDATE pessoas SET pessoa_tipo = 'PF' WHERE pessoa_tipo = 'MORADOR';
