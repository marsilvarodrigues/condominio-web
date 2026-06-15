-- liquibase formatted sql
-- changeset pmrodrigues:0043-refactor-historico-ocupacao dbms:postgresql

ALTER TABLE historico_ocupacao RENAME COLUMN criado_em TO created_at;

ALTER TABLE historico_ocupacao ADD COLUMN updated_at  TIMESTAMP;
ALTER TABLE historico_ocupacao ADD COLUMN created_by  VARCHAR(255);
ALTER TABLE historico_ocupacao ADD COLUMN updated_by  VARCHAR(255);

ALTER TABLE historico_ocupacao DROP COLUMN nome_morador;
ALTER TABLE historico_ocupacao DROP COLUMN email_morador;
ALTER TABLE historico_ocupacao DROP COLUMN cpf_morador;

-- rollback ALTER TABLE historico_ocupacao RENAME COLUMN created_at TO criado_em;
-- rollback ALTER TABLE historico_ocupacao DROP COLUMN updated_at;
-- rollback ALTER TABLE historico_ocupacao DROP COLUMN created_by;
-- rollback ALTER TABLE historico_ocupacao DROP COLUMN updated_by;
-- rollback ALTER TABLE historico_ocupacao ADD COLUMN nome_morador  VARCHAR(255) NOT NULL DEFAULT '';
-- rollback ALTER TABLE historico_ocupacao ADD COLUMN email_morador VARCHAR(255);
-- rollback ALTER TABLE historico_ocupacao ADD COLUMN cpf_morador   VARCHAR(14);
