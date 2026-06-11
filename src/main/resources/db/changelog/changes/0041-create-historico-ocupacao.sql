-- liquibase formatted sql
-- changeset pmrodrigues:0041-create-historico-ocupacao dbms:postgresql

CREATE TABLE historico_ocupacao (
    id              BIGSERIAL       PRIMARY KEY,
    condominio_id   BIGINT          NOT NULL,
    apartamento_id  BIGINT          NOT NULL,
    pessoa_id       BIGINT          NOT NULL,
    nome_morador    VARCHAR(255)    NOT NULL,
    email_morador   VARCHAR(255),
    cpf_morador     VARCHAR(14),
    data_entrada    DATE            NOT NULL,
    data_saida      DATE            NOT NULL,
    criado_em       TIMESTAMP       NOT NULL DEFAULT NOW()
)PARTITION BY HASH (condominio_id);

CREATE TABLE historico_ocupacao_p0 PARTITION OF historico_ocupacao FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE historico_ocupacao_p1 PARTITION OF historico_ocupacao FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE historico_ocupacao_p2 PARTITION OF historico_ocupacao FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE historico_ocupacao_p3 PARTITION OF historico_ocupacao FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_hist_ocupacao_apartamento ON historico_ocupacao (apartamento_id, condominio_id);
CREATE INDEX idx_hist_ocupacao_pessoa      ON historico_ocupacao (pessoa_id);
CREATE INDEX idx_hist_ocupacao_data_saida  ON historico_ocupacao (data_saida DESC);

-- rollback DROP TABLE historico_ocupacao;
