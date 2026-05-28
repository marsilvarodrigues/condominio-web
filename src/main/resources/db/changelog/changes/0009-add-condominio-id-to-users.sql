-- liquibase formatted sql
-- changeset pmrodrigues:0009-add-condominio-id-to-users

ALTER TABLE users ADD COLUMN condominio_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_condominio FOREIGN KEY (condominio_id) REFERENCES condominios(id);
