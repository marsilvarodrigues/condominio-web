-- liquibase formatted sql

-- changeset pmrodrigues:0026-create-despesas dbms:postgresql

CREATE SEQUENCE despesas_id_seq;

CREATE TABLE despesas (
    id                   BIGINT        NOT NULL DEFAULT nextval('despesas_id_seq'),
    condominio_id        BIGINT        NOT NULL,
    grupo_despesa_id     BIGINT        NOT NULL,
    descricao            VARCHAR(500)  NOT NULL,
    valor_total          NUMERIC(15,2) NOT NULL,
    competencia          DATE          NOT NULL,
    rateio_status        VARCHAR(10)   NOT NULL DEFAULT 'PENDENTE'
                         CHECK (rateio_status IN ('PENDENTE','RATEADA','ERRO')),
    data_ultimo_rateio   TIMESTAMP,
    deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP,
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE despesas_p0 PARTITION OF despesas FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE despesas_p1 PARTITION OF despesas FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE despesas_p2 PARTITION OF despesas FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE despesas_p3 PARTITION OF despesas FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_despesas_condominio     ON despesas (condominio_id);
CREATE INDEX idx_despesas_grupo          ON despesas (grupo_despesa_id, condominio_id);
CREATE INDEX idx_despesas_rateio_status  ON despesas (condominio_id, rateio_status)
    WHERE deleted = FALSE AND rateio_status IN ('PENDENTE','ERRO');

ALTER TABLE despesas
    ADD CONSTRAINT fk_despesas_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE despesas
    ADD CONSTRAINT fk_despesas_grupo_despesa
    FOREIGN KEY (grupo_despesa_id, condominio_id) REFERENCES grupos_despesa(id, condominio_id);

-- rollback DROP TABLE IF EXISTS despesas CASCADE; DROP SEQUENCE IF EXISTS despesas_id_seq;
