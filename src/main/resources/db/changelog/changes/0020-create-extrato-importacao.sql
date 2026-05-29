-- liquibase formatted sql

-- changeset pmrodrigues:0020-create-extrato-importacao dbms:postgresql

CREATE SEQUENCE extrato_importacoes_id_seq;
CREATE SEQUENCE itens_extrato_id_seq;

CREATE TABLE extrato_importacoes (
    id                BIGINT        NOT NULL DEFAULT nextval('extrato_importacoes_id_seq'),
    condominio_id     BIGINT        NOT NULL,
    conta_bancaria_id BIGINT        NOT NULL,
    formato           VARCHAR(10)   NOT NULL CHECK (formato IN ('OFX','CSV')),
    data_importacao   TIMESTAMP     NOT NULL,
    data_inicio       DATE,
    data_fim          DATE,
    total_itens       INTEGER       NOT NULL DEFAULT 0,
    itens_conciliados INTEGER       NOT NULL DEFAULT 0,
    itens_pendentes   INTEGER       NOT NULL DEFAULT 0,
    status            VARCHAR(15)   NOT NULL DEFAULT 'PROCESSANDO' CHECK (status IN ('PROCESSANDO','CONCLUIDO','ERRO')),
    mensagem_erro     TEXT,
    nome_arquivo      VARCHAR(500),
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE extrato_importacoes_p0 PARTITION OF extrato_importacoes FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE extrato_importacoes_p1 PARTITION OF extrato_importacoes FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE extrato_importacoes_p2 PARTITION OF extrato_importacoes FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE extrato_importacoes_p3 PARTITION OF extrato_importacoes FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_extrato_importacoes_condominio_id    ON extrato_importacoes (condominio_id);
CREATE INDEX idx_extrato_importacoes_conta_bancaria   ON extrato_importacoes (conta_bancaria_id);
CREATE INDEX idx_extrato_importacoes_data_importacao  ON extrato_importacoes (data_importacao);

ALTER TABLE extrato_importacoes
    ADD CONSTRAINT fk_extrato_importacoes_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE extrato_importacoes
    ADD CONSTRAINT fk_extrato_importacoes_conta_bancaria
    FOREIGN KEY (conta_bancaria_id, condominio_id) REFERENCES contas_bancarias(id, condominio_id);

-- ── itens_extrato ────────────────────────────────────────────────────────────

CREATE TABLE itens_extrato (
    id                    BIGINT        NOT NULL DEFAULT nextval('itens_extrato_id_seq'),
    condominio_id         BIGINT        NOT NULL,
    extrato_importacao_id BIGINT        NOT NULL,
    lancamento_id         BIGINT,
    data_lancamento       DATE          NOT NULL,
    valor                 NUMERIC(15,2) NOT NULL,
    tipo                  VARCHAR(10)   NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    descricao             VARCHAR(500),
    numero_documento      VARCHAR(100),
    status                VARCHAR(15)   NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','CONCILIADO','IGNORADO')),
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE itens_extrato_p0 PARTITION OF itens_extrato FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE itens_extrato_p1 PARTITION OF itens_extrato FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE itens_extrato_p2 PARTITION OF itens_extrato FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE itens_extrato_p3 PARTITION OF itens_extrato FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_itens_extrato_condominio_id         ON itens_extrato (condominio_id);
CREATE INDEX idx_itens_extrato_importacao_id         ON itens_extrato (extrato_importacao_id);
CREATE INDEX idx_itens_extrato_lancamento_id         ON itens_extrato (lancamento_id) WHERE lancamento_id IS NOT NULL;
CREATE INDEX idx_itens_extrato_status                ON itens_extrato (condominio_id, status);

ALTER TABLE itens_extrato
    ADD CONSTRAINT fk_itens_extrato_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE itens_extrato
    ADD CONSTRAINT fk_itens_extrato_importacao
    FOREIGN KEY (extrato_importacao_id, condominio_id) REFERENCES extrato_importacoes(id, condominio_id);

ALTER TABLE itens_extrato
    ADD CONSTRAINT fk_itens_extrato_lancamento
    FOREIGN KEY (lancamento_id, condominio_id) REFERENCES lancamentos_bancarios(id, condominio_id);

-- rollback DROP TABLE IF EXISTS itens_extrato CASCADE; DROP TABLE IF EXISTS extrato_importacoes CASCADE; DROP SEQUENCE IF EXISTS itens_extrato_id_seq, extrato_importacoes_id_seq;
