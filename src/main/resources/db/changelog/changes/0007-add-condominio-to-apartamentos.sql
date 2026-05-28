-- liquibase formatted sql
-- changeset pmrodrigues:0007-add-condominio-to-apartamentos

ALTER TABLE apartamentos ADD COLUMN condominio_id BIGINT NOT NULL;
ALTER TABLE apartamentos ADD CONSTRAINT fk_apartamentos_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);