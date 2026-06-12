-- liquibase formatted sql
-- changeset pmrodrigues:0042-add-fracao-ideal-andar-to-apartamentos

ALTER TABLE apartamentos ADD COLUMN IF NOT EXISTS fracao_ideal NUMERIC(10,6);
ALTER TABLE apartamentos ADD COLUMN IF NOT EXISTS andar INTEGER;
