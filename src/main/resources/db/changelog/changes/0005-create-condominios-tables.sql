-- liquibase formatted sql

-- changeset pmrodrigues:0005-create-condominios-tables
CREATE TABLE condominios (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL,
    cnpj        VARCHAR(18)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL,
    logradouro  VARCHAR(255) NOT NULL,
    cep         VARCHAR(8)   NOT NULL,
    cidade      VARCHAR(255) NOT NULL,
    estado_id   BIGINT       NOT NULL REFERENCES estados(id),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE TABLE blocos (
    id            BIGSERIAL   PRIMARY KEY,
    condominio_id BIGINT      NOT NULL REFERENCES condominios(id),
    numero        INTEGER     NOT NULL,
    bloco         VARCHAR(50) NOT NULL,
    deleted       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

CREATE TABLE apartamentos (
    id         BIGSERIAL   PRIMARY KEY,
    bloco_id   BIGINT      NOT NULL REFERENCES blocos(id),
    numero     VARCHAR(20) NOT NULL,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
-- rollback DROP TABLE apartamentos; DROP TABLE blocos; DROP TABLE condominios;