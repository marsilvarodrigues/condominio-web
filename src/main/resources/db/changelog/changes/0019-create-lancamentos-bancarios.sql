-- liquibase formatted sql

-- changeset pmrodrigues:0019-create-lancamentos-bancarios dbms:postgresql

CREATE SEQUENCE lancamentos_bancarios_id_seq;

CREATE TABLE lancamentos_bancarios (
    id               BIGINT        NOT NULL DEFAULT nextval('lancamentos_bancarios_id_seq'),
    condominio_id    BIGINT        NOT NULL,
    conta_bancaria_id BIGINT       NOT NULL,
    data_lancamento  DATE          NOT NULL,
    valor            NUMERIC(15,2) NOT NULL,
    tipo             VARCHAR(10)   NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    descricao        VARCHAR(500)  NOT NULL,
    origem           VARCHAR(30)   NOT NULL CHECK (origem IN ('COTA_CONDOMINIO','RESERVA','DESPESA_ORDINARIA','DESPESA_EXTRAORDINARIA','TAXA_EXTRA','MULTA','JUROS','MANUAL','IMPORTACAO')),
    referencia_id    BIGINT,
    status           VARCHAR(15)   NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','CONCILIADO')),
    deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE lancamentos_bancarios_p0 PARTITION OF lancamentos_bancarios FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE lancamentos_bancarios_p1 PARTITION OF lancamentos_bancarios FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE lancamentos_bancarios_p2 PARTITION OF lancamentos_bancarios FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE lancamentos_bancarios_p3 PARTITION OF lancamentos_bancarios FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_lancamentos_condominio_id      ON lancamentos_bancarios (condominio_id);
CREATE INDEX idx_lancamentos_conta_bancaria_id  ON lancamentos_bancarios (conta_bancaria_id);
CREATE INDEX idx_lancamentos_data               ON lancamentos_bancarios (data_lancamento);
CREATE INDEX idx_lancamentos_status             ON lancamentos_bancarios (condominio_id, status);

ALTER TABLE lancamentos_bancarios
    ADD CONSTRAINT fk_lancamentos_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE lancamentos_bancarios
    ADD CONSTRAINT fk_lancamentos_conta_bancaria
    FOREIGN KEY (conta_bancaria_id, condominio_id) REFERENCES contas_bancarias(id, condominio_id);

-- rollback DROP TABLE IF EXISTS lancamentos_bancarios CASCADE; DROP SEQUENCE IF EXISTS lancamentos_bancarios_id_seq;
