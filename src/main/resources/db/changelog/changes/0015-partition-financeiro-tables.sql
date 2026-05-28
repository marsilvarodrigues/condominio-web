-- liquibase formatted sql

-- changeset pmrodrigues:0015-partition-financeiro-tables dbms:postgresql

-- ── Step 1: Preserve existing data ──────────────────────────────────────────
CREATE TEMP TABLE plano_contas_bak         AS TABLE plano_contas;
CREATE TEMP TABLE fundo_reserva_bak        AS TABLE fundo_reserva;
CREATE TEMP TABLE fundo_reserva_mov_bak    AS TABLE fundo_reserva_movimentacao;
CREATE TEMP TABLE orcamento_anual_bak      AS TABLE orcamento_anual;
CREATE TEMP TABLE item_orcamento_bak       AS TABLE item_orcamento;

-- ── Step 2: Drop tables in FK order ─────────────────────────────────────────
DROP TABLE IF EXISTS item_orcamento             CASCADE;
DROP TABLE IF EXISTS orcamento_anual            CASCADE;
DROP TABLE IF EXISTS fundo_reserva_movimentacao CASCADE;
DROP TABLE IF EXISTS fundo_reserva              CASCADE;
DROP TABLE IF EXISTS plano_contas               CASCADE;

-- ── Step 3: Create sequences (resume from backed-up max id) ──────────────────
CREATE SEQUENCE plano_contas_id_seq;
CREATE SEQUENCE fundo_reserva_id_seq;
CREATE SEQUENCE fundo_reserva_movimentacao_id_seq;
CREATE SEQUENCE orcamento_anual_id_seq;
CREATE SEQUENCE item_orcamento_id_seq;

SELECT setval('plano_contas_id_seq',
              COALESCE((SELECT MAX(id) FROM plano_contas_bak), 0) + 1, false);
SELECT setval('fundo_reserva_id_seq',
              COALESCE((SELECT MAX(id) FROM fundo_reserva_bak), 0) + 1, false);
SELECT setval('fundo_reserva_movimentacao_id_seq',
              COALESCE((SELECT MAX(id) FROM fundo_reserva_mov_bak), 0) + 1, false);
SELECT setval('orcamento_anual_id_seq',
              COALESCE((SELECT MAX(id) FROM orcamento_anual_bak), 0) + 1, false);
SELECT setval('item_orcamento_id_seq',
              COALESCE((SELECT MAX(id) FROM item_orcamento_bak), 0) + 1, false);

-- ── Step 4: Partitioned plano_contas (HASH on condominio_id, 4 buckets) ──────
CREATE TABLE plano_contas (
    id            BIGINT       NOT NULL DEFAULT nextval('plano_contas_id_seq'),
    condominio_id BIGINT       NOT NULL,
    pai_id        BIGINT,
    codigo        VARCHAR(20)  NOT NULL,
    descricao     VARCHAR(255) NOT NULL,
    tipo          VARCHAR(10)  NOT NULL CHECK (tipo IN ('RECEITA','DESPESA')),
    tipo_rateio   VARCHAR(20)  CONSTRAINT plano_contas_tipo_rateio_check
                               CHECK (tipo_rateio IN ('IGUALITARIO','FRACAO_IDEAL')),
    escopo_rateio VARCHAR(20)  CONSTRAINT plano_contas_escopo_rateio_check
                               CHECK (escopo_rateio IN ('TODOS','POR_BLOCO')),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE plano_contas_p0 PARTITION OF plano_contas FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE plano_contas_p1 PARTITION OF plano_contas FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE plano_contas_p2 PARTITION OF plano_contas FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE plano_contas_p3 PARTITION OF plano_contas FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_plano_contas_condominio_id ON plano_contas (condominio_id);
CREATE INDEX idx_plano_contas_pai_id        ON plano_contas (pai_id) WHERE pai_id IS NOT NULL;

ALTER TABLE plano_contas
    ADD CONSTRAINT fk_plano_contas_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE plano_contas
    ADD CONSTRAINT fk_plano_contas_pai
    FOREIGN KEY (pai_id, condominio_id) REFERENCES plano_contas(id, condominio_id);

-- ── Step 5: Partitioned fundo_reserva (HASH on condominio_id, 4 buckets) ─────
CREATE TABLE fundo_reserva (
    id                     BIGINT        NOT NULL DEFAULT nextval('fundo_reserva_id_seq'),
    condominio_id          BIGINT        NOT NULL,
    percentual_arrecadacao NUMERIC(5,2)  NOT NULL CHECK (percentual_arrecadacao BETWEEN 0 AND 100),
    saldo_atual            NUMERIC(15,2) NOT NULL DEFAULT 0,
    conta_bancaria_destino VARCHAR(100),
    deleted                BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,
    created_by             VARCHAR(255),
    updated_by             VARCHAR(255),
    PRIMARY KEY (id, condominio_id),
    UNIQUE (condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE fundo_reserva_p0 PARTITION OF fundo_reserva FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE fundo_reserva_p1 PARTITION OF fundo_reserva FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE fundo_reserva_p2 PARTITION OF fundo_reserva FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE fundo_reserva_p3 PARTITION OF fundo_reserva FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_fundo_reserva_condominio_id ON fundo_reserva (condominio_id);

ALTER TABLE fundo_reserva
    ADD CONSTRAINT fk_fundo_reserva_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

-- ── Step 6: Partitioned fundo_reserva_movimentacao ───────────────────────────
CREATE TABLE fundo_reserva_movimentacao (
    id                BIGINT        NOT NULL DEFAULT nextval('fundo_reserva_movimentacao_id_seq'),
    condominio_id     BIGINT        NOT NULL,
    fundo_reserva_id  BIGINT        NOT NULL,
    tipo              VARCHAR(10)   NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    valor             NUMERIC(15,2) NOT NULL,
    justificativa     TEXT,
    data_movimentacao DATE          NOT NULL,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE fundo_reserva_movimentacao_p0 PARTITION OF fundo_reserva_movimentacao FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE fundo_reserva_movimentacao_p1 PARTITION OF fundo_reserva_movimentacao FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE fundo_reserva_movimentacao_p2 PARTITION OF fundo_reserva_movimentacao FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE fundo_reserva_movimentacao_p3 PARTITION OF fundo_reserva_movimentacao FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_fundo_movimentacao_condominio_id ON fundo_reserva_movimentacao (condominio_id);
CREATE INDEX idx_fundo_movimentacao_fundo_id      ON fundo_reserva_movimentacao (fundo_reserva_id);

ALTER TABLE fundo_reserva_movimentacao
    ADD CONSTRAINT fk_fundo_movimentacao_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE fundo_reserva_movimentacao
    ADD CONSTRAINT fk_fundo_movimentacao_fundo_reserva
    FOREIGN KEY (fundo_reserva_id, condominio_id) REFERENCES fundo_reserva(id, condominio_id);

-- ── Step 7: Partitioned orcamento_anual (HASH on condominio_id, 4 buckets) ───
CREATE TABLE orcamento_anual (
    id                    BIGINT        NOT NULL DEFAULT nextval('orcamento_anual_id_seq'),
    condominio_id         BIGINT        NOT NULL,
    exercicio             INTEGER       NOT NULL,
    status                VARCHAR(15)   NOT NULL DEFAULT 'RASCUNHO'
                                        CHECK (status IN ('RASCUNHO','APROVADO','ENCERRADO')),
    taxa_estimada_unidade NUMERIC(15,2),
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE orcamento_anual_p0 PARTITION OF orcamento_anual FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE orcamento_anual_p1 PARTITION OF orcamento_anual FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE orcamento_anual_p2 PARTITION OF orcamento_anual FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE orcamento_anual_p3 PARTITION OF orcamento_anual FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_orcamento_condominio_id ON orcamento_anual (condominio_id);
CREATE INDEX idx_orcamento_exercicio     ON orcamento_anual (exercicio);

ALTER TABLE orcamento_anual
    ADD CONSTRAINT fk_orcamento_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

-- ── Step 8: Partitioned item_orcamento (HASH on condominio_id, 4 buckets) ────
CREATE TABLE item_orcamento (
    id                 BIGINT        NOT NULL DEFAULT nextval('item_orcamento_id_seq'),
    condominio_id      BIGINT        NOT NULL,
    orcamento_anual_id BIGINT        NOT NULL,
    plano_contas_id    BIGINT        NOT NULL,
    valor_previsto     NUMERIC(15,2) NOT NULL,
    valor_realizado    NUMERIC(15,2) NOT NULL DEFAULT 0,
    deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE item_orcamento_p0 PARTITION OF item_orcamento FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE item_orcamento_p1 PARTITION OF item_orcamento FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE item_orcamento_p2 PARTITION OF item_orcamento FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE item_orcamento_p3 PARTITION OF item_orcamento FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_item_orcamento_condominio_id   ON item_orcamento (condominio_id);
CREATE INDEX idx_item_orcamento_orcamento_id    ON item_orcamento (orcamento_anual_id);
CREATE INDEX idx_item_orcamento_plano_contas_id ON item_orcamento (plano_contas_id);

ALTER TABLE item_orcamento
    ADD CONSTRAINT fk_item_orcamento_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE item_orcamento
    ADD CONSTRAINT fk_item_orcamento_orcamento_anual
    FOREIGN KEY (orcamento_anual_id, condominio_id) REFERENCES orcamento_anual(id, condominio_id);

ALTER TABLE item_orcamento
    ADD CONSTRAINT fk_item_orcamento_plano_contas
    FOREIGN KEY (plano_contas_id, condominio_id) REFERENCES plano_contas(id, condominio_id);

-- ── Step 9: Restore data ─────────────────────────────────────────────────────
INSERT INTO plano_contas
    (id, condominio_id, pai_id, codigo, descricao, tipo,
     tipo_rateio, escopo_rateio, deleted, created_at, updated_at, created_by, updated_by)
    SELECT id, condominio_id, pai_id, codigo, descricao, tipo,
           tipo_rateio, escopo_rateio, deleted, created_at, updated_at, created_by, updated_by
    FROM plano_contas_bak;

INSERT INTO fundo_reserva
    (id, condominio_id, percentual_arrecadacao, saldo_atual, conta_bancaria_destino,
     deleted, created_at, updated_at, created_by, updated_by)
    SELECT id, condominio_id, percentual_arrecadacao, saldo_atual, conta_bancaria_destino,
           deleted, created_at, updated_at, created_by, updated_by
    FROM fundo_reserva_bak;

INSERT INTO fundo_reserva_movimentacao
    (id, condominio_id, fundo_reserva_id, tipo, valor, justificativa,
     data_movimentacao, deleted, created_at, updated_at)
    SELECT id, condominio_id, fundo_reserva_id, tipo, valor, justificativa,
           data_movimentacao, deleted, created_at, updated_at
    FROM fundo_reserva_mov_bak;

INSERT INTO orcamento_anual
    (id, condominio_id, exercicio, status, taxa_estimada_unidade,
     deleted, created_at, updated_at, created_by, updated_by)
    SELECT id, condominio_id, exercicio, status, taxa_estimada_unidade,
           deleted, created_at, updated_at, created_by, updated_by
    FROM orcamento_anual_bak;

INSERT INTO item_orcamento
    (id, condominio_id, orcamento_anual_id, plano_contas_id, valor_previsto, valor_realizado,
     deleted, created_at, updated_at)
    SELECT id, condominio_id, orcamento_anual_id, plano_contas_id, valor_previsto, valor_realizado,
           deleted, created_at, updated_at
    FROM item_orcamento_bak;

-- rollback DROP TABLE item_orcamento CASCADE; DROP TABLE orcamento_anual CASCADE; DROP TABLE fundo_reserva_movimentacao CASCADE; DROP TABLE fundo_reserva CASCADE; DROP TABLE plano_contas CASCADE; DROP SEQUENCE IF EXISTS item_orcamento_id_seq, orcamento_anual_id_seq, fundo_reserva_movimentacao_id_seq, fundo_reserva_id_seq, plano_contas_id_seq;
