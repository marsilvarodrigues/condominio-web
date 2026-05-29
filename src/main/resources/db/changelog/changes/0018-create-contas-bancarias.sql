-- liquibase formatted sql

-- changeset pmrodrigues:0018-create-contas-bancarias dbms:postgresql

CREATE SEQUENCE contas_bancarias_id_seq;

CREATE TABLE contas_bancarias (
    id              BIGINT        NOT NULL DEFAULT nextval('contas_bancarias_id_seq'),
    condominio_id   BIGINT        NOT NULL,
    banco_id        BIGINT        NOT NULL,
    tipo            VARCHAR(15)   NOT NULL CHECK (tipo IN ('CORRENTE','POUPANCA','FUNDO_RESERVA')),
    agencia         VARCHAR(10)   NOT NULL,
    conta           VARCHAR(20)   NOT NULL,
    digito          VARCHAR(2),
    descricao       VARCHAR(255),
    chave_pix       VARCHAR(255),
    saldo_contabil  NUMERIC(15,2) NOT NULL DEFAULT 0,
    ativa           BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE contas_bancarias_p0 PARTITION OF contas_bancarias FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE contas_bancarias_p1 PARTITION OF contas_bancarias FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE contas_bancarias_p2 PARTITION OF contas_bancarias FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE contas_bancarias_p3 PARTITION OF contas_bancarias FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_contas_bancarias_condominio_id ON contas_bancarias (condominio_id);
CREATE INDEX idx_contas_bancarias_banco_id      ON contas_bancarias (banco_id);
CREATE INDEX idx_contas_bancarias_tipo          ON contas_bancarias (condominio_id, tipo);

-- Ensure at most one FUNDO_RESERVA account per condominium
CREATE UNIQUE INDEX uidx_contas_bancarias_fundo_reserva
    ON contas_bancarias (condominio_id)
    WHERE tipo = 'FUNDO_RESERVA' AND deleted = FALSE;

ALTER TABLE contas_bancarias
    ADD CONSTRAINT fk_contas_bancarias_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE contas_bancarias
    ADD CONSTRAINT fk_contas_bancarias_banco
    FOREIGN KEY (banco_id) REFERENCES bancos(id);

-- rollback DROP TABLE IF EXISTS contas_bancarias CASCADE; DROP SEQUENCE IF EXISTS contas_bancarias_id_seq;
