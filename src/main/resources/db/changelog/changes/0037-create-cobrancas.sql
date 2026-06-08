-- liquibase formatted sql
-- changeset pmrodrigues:0037-create-cobrancas dbms:postgresql

CREATE TABLE cobrancas (
    id                   BIGSERIAL,
    condominio_id        BIGINT        NOT NULL,
    apartamento_id       BIGINT        NOT NULL,
    cota_rateio_id       BIGINT        NOT NULL,
    morador_id           BIGINT,
    valor                NUMERIC(15,2) NOT NULL,
    vencimento           DATE          NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    asaas_id             VARCHAR(100),
    asaas_customer_id    VARCHAR(100),
    boleto_url           VARCHAR(500),
    boleto_codigo_barras VARCHAR(100),
    pix_qr_code_base64   TEXT,
    pix_copia_cola       TEXT,
    email_enviado        BOOLEAN       NOT NULL DEFAULT FALSE,
    email_enviado_em     TIMESTAMP,
    pago_em              TIMESTAMP,
    deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE cobrancas_p0 PARTITION OF cobrancas FOR VALUES WITH (modulus 4, remainder 0);
CREATE TABLE cobrancas_p1 PARTITION OF cobrancas FOR VALUES WITH (modulus 4, remainder 1);
CREATE TABLE cobrancas_p2 PARTITION OF cobrancas FOR VALUES WITH (modulus 4, remainder 2);
CREATE TABLE cobrancas_p3 PARTITION OF cobrancas FOR VALUES WITH (modulus 4, remainder 3);

CREATE INDEX idx_cobrancas_condominio    ON cobrancas (condominio_id);
CREATE INDEX idx_cobrancas_apartamento   ON cobrancas (apartamento_id, condominio_id);
CREATE INDEX idx_cobrancas_status        ON cobrancas (status, condominio_id);
CREATE INDEX idx_cobrancas_vencimento    ON cobrancas (vencimento, condominio_id);
CREATE INDEX idx_cobrancas_asaas_id      ON cobrancas (asaas_id) WHERE asaas_id IS NOT NULL;
CREATE UNIQUE INDEX idx_cobrancas_cota   ON cobrancas (cota_rateio_id, condominio_id);
