-- liquibase formatted sql

-- changeset pmrodrigues:0028-create-cotas-rateio dbms:postgresql

CREATE SEQUENCE cotas_rateio_id_seq;

CREATE TABLE cotas_rateio (
    id                  BIGINT         NOT NULL DEFAULT nextval('cotas_rateio_id_seq'),
    condominio_id       BIGINT         NOT NULL,
    despesa_id          BIGINT         NOT NULL,
    apartamento_id      BIGINT         NOT NULL,
    valor               NUMERIC(15,2)  NOT NULL,
    rateio_execucao_id  BIGINT         NOT NULL,
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE cotas_rateio_p0 PARTITION OF cotas_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE cotas_rateio_p1 PARTITION OF cotas_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE cotas_rateio_p2 PARTITION OF cotas_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE cotas_rateio_p3 PARTITION OF cotas_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_cotas_rateio_condominio ON cotas_rateio (condominio_id);
CREATE INDEX idx_cotas_rateio_despesa    ON cotas_rateio (despesa_id, condominio_id);
CREATE INDEX idx_cotas_rateio_apto       ON cotas_rateio (apartamento_id, condominio_id);

ALTER TABLE cotas_rateio
    ADD CONSTRAINT fk_cotas_rateio_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE cotas_rateio
    ADD CONSTRAINT fk_cotas_rateio_despesa
    FOREIGN KEY (despesa_id, condominio_id) REFERENCES despesas(id, condominio_id);

ALTER TABLE cotas_rateio
    ADD CONSTRAINT fk_cotas_rateio_apartamento
    FOREIGN KEY (apartamento_id, condominio_id) REFERENCES apartamentos(id, condominio_id);

ALTER TABLE cotas_rateio
    ADD CONSTRAINT fk_cotas_rateio_execucao
    FOREIGN KEY (rateio_execucao_id, condominio_id) REFERENCES rateio_execucoes(id, condominio_id);

-- rollback DROP TABLE IF EXISTS cotas_rateio CASCADE; DROP SEQUENCE IF EXISTS cotas_rateio_id_seq;
