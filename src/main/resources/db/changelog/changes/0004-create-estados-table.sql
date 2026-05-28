-- liquibase formatted sql

-- changeset pmrodrigues:0004-create-estados-table
CREATE TABLE estados (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    uf   CHAR(2)      NOT NULL UNIQUE
);
-- rollback DROP TABLE estados;