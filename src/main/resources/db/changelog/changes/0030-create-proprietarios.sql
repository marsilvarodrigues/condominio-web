-- liquibase formatted sql
-- changeset pmrodrigues:0030-create-proprietario-apartamentos dbms:postgresql

-- Simple join table for the Proprietario <-> Apartamento many-to-many relationship.
-- Only proprietario_id and apartamento_id are managed by Hibernate @JoinTable.
-- No condominio_id here: tenant scoping is enforced through the Proprietario (Pessoa)
-- and Apartamento entities, both of which carry their own condominio_id.
-- No FK to apartamentos: apartamentos has a composite PK (id, condominio_id) due to
-- hash partitioning; PostgreSQL requires all FK columns to form a unique constraint,
-- so a single-column FK to apartamentos(id) is not possible.
CREATE TABLE proprietario_apartamentos (
    proprietario_id BIGINT NOT NULL,
    apartamento_id  BIGINT NOT NULL,
    PRIMARY KEY (proprietario_id, apartamento_id)
);

CREATE INDEX idx_propapt_apartamento ON proprietario_apartamentos (apartamento_id);

ALTER TABLE proprietario_apartamentos ADD CONSTRAINT fk_propapt_proprietario
    FOREIGN KEY (proprietario_id) REFERENCES pessoas(id);

-- rollback DROP TABLE IF EXISTS proprietario_apartamentos CASCADE;
