-- liquibase formatted sql

-- changeset pmrodrigues:0027-create-rateio-execucao dbms:postgresql

CREATE SEQUENCE rateio_execucoes_id_seq;

CREATE TABLE rateio_execucoes (
    id                BIGINT         NOT NULL DEFAULT nextval('rateio_execucoes_id_seq'),
    condominio_id     BIGINT         NOT NULL,
    despesa_id        BIGINT         NOT NULL,
    grupo_despesa_id  BIGINT         NOT NULL,
    tipo_execucao     VARCHAR(15)    NOT NULL CHECK (tipo_execucao IN ('AUTOMATICO','MANUAL','RECALCULO')),
    data_execucao     TIMESTAMP      NOT NULL DEFAULT NOW(),
    despesa_total     NUMERIC(15,2)  NOT NULL,
    total_unidades    INTEGER,
    total_cotas       NUMERIC(15,2),
    status            VARCHAR(10)    NOT NULL CHECK (status IN ('SUCESSO','ERRO','PARCIAL')),
    erro_mensagem     TEXT,
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE rateio_execucoes_p0 PARTITION OF rateio_execucoes FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE rateio_execucoes_p1 PARTITION OF rateio_execucoes FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE rateio_execucoes_p2 PARTITION OF rateio_execucoes FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE rateio_execucoes_p3 PARTITION OF rateio_execucoes FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_rateio_exec_condominio ON rateio_execucoes (condominio_id);
CREATE INDEX idx_rateio_exec_despesa    ON rateio_execucoes (despesa_id, condominio_id);
CREATE INDEX idx_rateio_exec_data       ON rateio_execucoes (data_execucao DESC);

ALTER TABLE rateio_execucoes
    ADD CONSTRAINT fk_rateio_exec_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE rateio_execucoes
    ADD CONSTRAINT fk_rateio_exec_despesa
    FOREIGN KEY (despesa_id, condominio_id) REFERENCES despesas(id, condominio_id);

-- rollback DROP TABLE IF EXISTS rateio_execucoes CASCADE; DROP SEQUENCE IF EXISTS rateio_execucoes_id_seq;
