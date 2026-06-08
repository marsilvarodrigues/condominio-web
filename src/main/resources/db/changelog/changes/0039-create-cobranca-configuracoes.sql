-- liquibase formatted sql
-- changeset pmrodrigues:0039-create-cobranca-configuracoes dbms:postgresql

-- Per-condominium billing configuration. One row per condominium.
-- Falls back to application.yaml defaults when no row exists for a given tenant.
CREATE TABLE cobranca_configuracoes (
    id                   BIGSERIAL    PRIMARY KEY,
    condominio_id        BIGINT       NOT NULL UNIQUE,
    vencimento_dias      INTEGER      NOT NULL DEFAULT 10,
    juros_mora_percent   DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    multa_percent        DECIMAL(5,2) NOT NULL DEFAULT 2.00,
    descricao_padrao     VARCHAR(255) NOT NULL DEFAULT 'Taxa condominial',
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    CONSTRAINT fk_cobranca_conf_condominio FOREIGN KEY (condominio_id) REFERENCES condominios (id)
);

CREATE INDEX idx_cobranca_conf_condominio ON cobranca_configuracoes (condominio_id);
