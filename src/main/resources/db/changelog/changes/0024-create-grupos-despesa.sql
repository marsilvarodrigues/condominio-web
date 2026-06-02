-- liquibase formatted sql

-- changeset pmrodrigues:0024-create-grupos-despesa dbms:postgresql

CREATE SEQUENCE grupos_despesa_id_seq;

CREATE TABLE grupos_despesa (
    id               BIGINT        NOT NULL DEFAULT nextval('grupos_despesa_id_seq'),
    condominio_id    BIGINT        NOT NULL,
    nome             VARCHAR(255)  NOT NULL,
    tipo_rateio      VARCHAR(20)   NOT NULL CHECK (tipo_rateio IN ('IGUALITARIO','FRACAO_IDEAL','METRAGEM','CONSUMO')),
    escopo           VARCHAR(20)   NOT NULL DEFAULT 'TODOS' CHECK (escopo IN ('TODOS','POR_BLOCO')),
    bloco_id         BIGINT,
    plano_contas_id  BIGINT,
    parametros_json  JSONB,
    deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE grupos_despesa_p0 PARTITION OF grupos_despesa FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE grupos_despesa_p1 PARTITION OF grupos_despesa FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE grupos_despesa_p2 PARTITION OF grupos_despesa FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE grupos_despesa_p3 PARTITION OF grupos_despesa FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_grupos_despesa_condominio ON grupos_despesa (condominio_id);
CREATE INDEX idx_grupos_despesa_tipo       ON grupos_despesa (condominio_id, tipo_rateio);
CREATE INDEX idx_grupos_despesa_escopo     ON grupos_despesa (condominio_id, escopo);

ALTER TABLE grupos_despesa
    ADD CONSTRAINT fk_grupos_despesa_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

-- rollback DROP TABLE IF EXISTS grupos_despesa CASCADE; DROP SEQUENCE IF EXISTS grupos_despesa_id_seq;
