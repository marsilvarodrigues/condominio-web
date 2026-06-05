-- liquibase formatted sql
-- changeset pmrodrigues:0029-create-pessoas dbms:postgresql

-- Pessoas table: JOINED from users.
-- Soft-delete (deleted) and timestamps (created_at, updated_at) are inherited from users.
-- pessoa_tipo is the SINGLE_TABLE discriminator for the Pessoa sub-hierarchy.
CREATE TABLE pessoas (
    id              BIGINT       NOT NULL,
    condominio_id   BIGINT       NOT NULL,
    nome            VARCHAR(150) NOT NULL,
    apartamento_id  BIGINT,
    pessoa_tipo     VARCHAR(20)  NOT NULL,   -- SINGLE_TABLE discriminator: PF, PJ, PROP_PF, PROP_PJ
    telefone        VARCHAR(20),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES users(id)
);

CREATE INDEX idx_pessoas_condominio   ON pessoas (condominio_id);
CREATE INDEX idx_pessoas_apartamento  ON pessoas (apartamento_id) WHERE apartamento_id IS NOT NULL;
CREATE INDEX idx_pessoas_tipo         ON pessoas (pessoa_tipo);

ALTER TABLE pessoas ADD CONSTRAINT fk_pessoas_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE pessoas ADD CONSTRAINT fk_pessoas_apartamento
    FOREIGN KEY (apartamento_id, condominio_id) REFERENCES apartamentos(id, condominio_id);

-- PessoaFisica columns (SINGLE_TABLE, nullable for PJ/PROP types)
ALTER TABLE pessoas ADD COLUMN cpf VARCHAR(14);
CREATE INDEX idx_pessoas_cpf ON pessoas (cpf) WHERE cpf IS NOT NULL;

-- PessoaJuridica + ProprietarioPessoaJuridica columns
ALTER TABLE pessoas ADD COLUMN cnpj         VARCHAR(18);
ALTER TABLE pessoas ADD COLUMN razao_social VARCHAR(255);
CREATE INDEX idx_pessoas_cnpj ON pessoas (cnpj) WHERE cnpj IS NOT NULL;

-- rollback DROP TABLE IF EXISTS pessoas CASCADE;
