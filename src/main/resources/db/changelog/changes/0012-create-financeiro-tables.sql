-- liquibase formatted sql

-- changeset pmrodrigues:0012-create-financeiro-tables

CREATE TABLE plano_contas (
    id            BIGSERIAL    PRIMARY KEY,
    condominio_id BIGINT       NOT NULL REFERENCES condominios(id),
    pai_id        BIGINT       REFERENCES plano_contas(id),
    codigo        VARCHAR(20)  NOT NULL,
    descricao     VARCHAR(255) NOT NULL,
    tipo          VARCHAR(10)  NOT NULL CHECK (tipo IN ('RECEITA','DESPESA')),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

CREATE INDEX idx_plano_contas_condominio_id ON plano_contas (condominio_id);
CREATE INDEX idx_plano_contas_pai_id        ON plano_contas (pai_id);

CREATE TABLE fundo_reserva (
    id                       BIGSERIAL       PRIMARY KEY,
    condominio_id            BIGINT          NOT NULL UNIQUE REFERENCES condominios(id),
    percentual_arrecadacao   NUMERIC(5,2)    NOT NULL CHECK (percentual_arrecadacao BETWEEN 0 AND 100),
    saldo_atual              NUMERIC(15,2)   NOT NULL DEFAULT 0,
    conta_bancaria_destino   VARCHAR(100),
    deleted                  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255)
);

CREATE INDEX idx_fundo_reserva_condominio_id ON fundo_reserva (condominio_id);

CREATE TABLE fundo_reserva_movimentacao (
    id               BIGSERIAL      PRIMARY KEY,
    fundo_reserva_id BIGINT         NOT NULL REFERENCES fundo_reserva(id),
    condominio_id    BIGINT         NOT NULL,
    tipo             VARCHAR(10)    NOT NULL CHECK (tipo IN ('CREDITO','DEBITO')),
    valor            NUMERIC(15,2)  NOT NULL,
    justificativa    TEXT,
    data_movimentacao DATE          NOT NULL,
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP,
    updated_at       TIMESTAMP
);

CREATE INDEX idx_fundo_movimentacao_fundo_id      ON fundo_reserva_movimentacao (fundo_reserva_id);
CREATE INDEX idx_fundo_movimentacao_condominio_id  ON fundo_reserva_movimentacao (condominio_id);

CREATE TABLE orcamento_anual (
    id                    BIGSERIAL     PRIMARY KEY,
    condominio_id         BIGINT        NOT NULL REFERENCES condominios(id),
    exercicio             INTEGER       NOT NULL,
    status                VARCHAR(15)   NOT NULL DEFAULT 'RASCUNHO' CHECK (status IN ('RASCUNHO','APROVADO','ENCERRADO')),
    taxa_estimada_unidade NUMERIC(15,2),
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP,
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX idx_orcamento_condominio_id ON orcamento_anual (condominio_id);
CREATE INDEX idx_orcamento_exercicio     ON orcamento_anual (exercicio);

CREATE TABLE item_orcamento (
    id                BIGSERIAL     PRIMARY KEY,
    orcamento_anual_id BIGINT       NOT NULL REFERENCES orcamento_anual(id),
    condominio_id      BIGINT       NOT NULL,
    plano_contas_id    BIGINT       NOT NULL REFERENCES plano_contas(id),
    valor_previsto     NUMERIC(15,2) NOT NULL,
    valor_realizado    NUMERIC(15,2) NOT NULL DEFAULT 0,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE INDEX idx_item_orcamento_orcamento_id  ON item_orcamento (orcamento_anual_id);
CREATE INDEX idx_item_orcamento_condominio_id ON item_orcamento (condominio_id);

-- rollback DROP TABLE item_orcamento; DROP TABLE orcamento_anual; DROP TABLE fundo_reserva_movimentacao; DROP TABLE fundo_reserva; DROP TABLE plano_contas;
