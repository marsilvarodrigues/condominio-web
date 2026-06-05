-- liquibase formatted sql
-- changeset pmrodrigues:0036-create-pessoa-subtype-tables dbms:postgresql

-- Pessoa hierarchy converted from mixed SINGLE_TABLE+JOINED to pure JOINED inheritance.
-- Each concrete subtype now has its own table joined to pessoas(id).

CREATE TABLE moradores (
    id  BIGINT       NOT NULL,
    cpf VARCHAR(14),
    CONSTRAINT pk_moradores PRIMARY KEY (id),
    CONSTRAINT fk_moradores_pessoa FOREIGN KEY (id) REFERENCES pessoas(id)
);
CREATE INDEX idx_moradores_cpf ON moradores (cpf) WHERE cpf IS NOT NULL;

CREATE TABLE proprietarios (
    id BIGINT NOT NULL,
    CONSTRAINT pk_proprietarios PRIMARY KEY (id),
    CONSTRAINT fk_proprietarios_pessoa FOREIGN KEY (id) REFERENCES pessoas(id)
);

CREATE TABLE proprietario_pf (
    id  BIGINT      NOT NULL,
    cpf VARCHAR(14),
    CONSTRAINT pk_proprietario_pf PRIMARY KEY (id),
    CONSTRAINT fk_proprietario_pf_prop FOREIGN KEY (id) REFERENCES proprietarios(id)
);
CREATE INDEX idx_proprietario_pf_cpf ON proprietario_pf (cpf) WHERE cpf IS NOT NULL;

CREATE TABLE proprietario_pj (
    id           BIGINT       NOT NULL,
    cnpj         VARCHAR(18),
    razao_social VARCHAR(255),
    CONSTRAINT pk_proprietario_pj PRIMARY KEY (id),
    CONSTRAINT fk_proprietario_pj_prop FOREIGN KEY (id) REFERENCES proprietarios(id)
);
CREATE INDEX idx_proprietario_pj_cnpj ON proprietario_pj (cnpj) WHERE cnpj IS NOT NULL;

-- Migrate existing data from pessoas columns to type-specific tables
INSERT INTO moradores (id, cpf)
SELECT id, cpf FROM pessoas WHERE pessoa_tipo = 'MORADOR';

INSERT INTO proprietarios (id)
SELECT id FROM pessoas WHERE pessoa_tipo IN ('PROP', 'PROP_PF', 'PROP_PJ');

INSERT INTO proprietario_pf (id, cpf)
SELECT id, cpf FROM pessoas WHERE pessoa_tipo = 'PROP_PF';

INSERT INTO proprietario_pj (id, cnpj, razao_social)
SELECT id, cnpj, razao_social FROM pessoas WHERE pessoa_tipo = 'PROP_PJ';

-- Drop type-specific columns from pessoas (now owned by subtype tables)
ALTER TABLE pessoas DROP COLUMN IF EXISTS cpf;
ALTER TABLE pessoas DROP COLUMN IF EXISTS cnpj;
ALTER TABLE pessoas DROP COLUMN IF EXISTS razao_social;
-- pessoa_tipo is kept as the JOINED inheritance discriminator column for Pessoa hierarchy

-- rollback DROP TABLE IF EXISTS proprietario_pj CASCADE;
-- rollback DROP TABLE IF EXISTS proprietario_pf CASCADE;
-- rollback DROP TABLE IF EXISTS proprietarios CASCADE;
-- rollback DROP TABLE IF EXISTS moradores CASCADE;
