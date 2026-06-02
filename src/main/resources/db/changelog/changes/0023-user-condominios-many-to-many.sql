-- liquibase formatted sql
-- changeset pmrodrigues:0023-user-condominios-many-to-many

CREATE TABLE user_condominios (
    user_id       BIGINT NOT NULL,
    condominio_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, condominio_id),
    CONSTRAINT fk_uc_user       FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_uc_condominio FOREIGN KEY (condominio_id) REFERENCES condominios(id)
);

-- Migrate existing single condominio_id associations
INSERT INTO user_condominios (user_id, condominio_id)
SELECT id, condominio_id FROM users WHERE condominio_id IS NOT NULL;

ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_condominio;
ALTER TABLE users DROP COLUMN IF EXISTS condominio_id;
