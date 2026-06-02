-- liquibase formatted sql

-- changeset pmrodrigues:0025-create-coeficientes-rateio dbms:postgresql

CREATE SEQUENCE coeficientes_rateio_id_seq;

CREATE TABLE coeficientes_rateio (
    id                BIGINT         NOT NULL DEFAULT nextval('coeficientes_rateio_id_seq'),
    condominio_id     BIGINT         NOT NULL,
    grupo_despesa_id  BIGINT         NOT NULL,
    apartamento_id    BIGINT         NOT NULL,
    coeficiente       NUMERIC(10,6),
    area_m2           NUMERIC(8,2),
    consumo_m3        NUMERIC(10,3),
    vigencia          DATE           NOT NULL,
    deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE coeficientes_rateio_p0 PARTITION OF coeficientes_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE coeficientes_rateio_p1 PARTITION OF coeficientes_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE coeficientes_rateio_p2 PARTITION OF coeficientes_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE coeficientes_rateio_p3 PARTITION OF coeficientes_rateio FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_coef_rateio_condominio ON coeficientes_rateio (condominio_id);
CREATE INDEX idx_coef_rateio_grupo      ON coeficientes_rateio (grupo_despesa_id, condominio_id);
CREATE INDEX idx_coef_rateio_vigencia   ON coeficientes_rateio (grupo_despesa_id, condominio_id, vigencia DESC)
    WHERE deleted = FALSE;

ALTER TABLE coeficientes_rateio
    ADD CONSTRAINT fk_coef_rateio_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios(id);

ALTER TABLE coeficientes_rateio
    ADD CONSTRAINT fk_coef_rateio_grupo
    FOREIGN KEY (grupo_despesa_id, condominio_id) REFERENCES grupos_despesa(id, condominio_id);

ALTER TABLE coeficientes_rateio
    ADD CONSTRAINT fk_coef_rateio_apartamento
    FOREIGN KEY (apartamento_id, condominio_id) REFERENCES apartamentos(id, condominio_id);

-- rollback DROP TABLE IF EXISTS coeficientes_rateio CASCADE; DROP SEQUENCE IF EXISTS coeficientes_rateio_id_seq;
