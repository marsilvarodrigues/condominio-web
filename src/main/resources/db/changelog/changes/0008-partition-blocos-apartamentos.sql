-- liquibase formatted sql
-- changeset pmrodrigues:0008-partition-blocos-apartamentos dbms:postgresql

-- ── Step 1: Preserve existing data ────────────────────────────────────────
CREATE TEMP TABLE blocos_bak        AS TABLE blocos;
CREATE TEMP TABLE apartamentos_bak  AS TABLE apartamentos;

-- ── Step 2: Drop existing tables (removes sequences owned by BIGSERIAL) ───
DROP TABLE IF EXISTS apartamentos CASCADE;
DROP TABLE IF EXISTS blocos        CASCADE;

-- ── Step 3: New sequences (resume from backed-up max id) ──────────────────
CREATE SEQUENCE blocos_id_seq;
CREATE SEQUENCE apartamentos_id_seq;

SELECT setval('blocos_id_seq',
              COALESCE((SELECT MAX(id) FROM blocos_bak), 0) + 1, false);
SELECT setval('apartamentos_id_seq',
              COALESCE((SELECT MAX(id) FROM apartamentos_bak), 0) + 1, false);

-- ── Step 4: Partitioned blocos (HASH on condominio_id, 4 buckets) ─────────
CREATE TABLE blocos (
    id            BIGINT       NOT NULL DEFAULT nextval('blocos_id_seq'),
    condominio_id BIGINT       NOT NULL,
    numero        INTEGER      NOT NULL,
    bloco         VARCHAR(50)  NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE blocos_p0 PARTITION OF blocos FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE blocos_p1 PARTITION OF blocos FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE blocos_p2 PARTITION OF blocos FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE blocos_p3 PARTITION OF blocos FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_blocos_condominio_id ON blocos (condominio_id);

ALTER TABLE blocos
    ADD CONSTRAINT fk_blocos_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios (id);

-- ── Step 5: Partitioned apartamentos (HASH on condominio_id, 4 buckets) ───
CREATE TABLE apartamentos (
    id            BIGINT       NOT NULL DEFAULT nextval('apartamentos_id_seq'),
    condominio_id BIGINT       NOT NULL,
    bloco_id      BIGINT       NOT NULL,
    numero        VARCHAR(20)  NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    PRIMARY KEY (id, condominio_id)
) PARTITION BY HASH (condominio_id);

CREATE TABLE apartamentos_p0 PARTITION OF apartamentos FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE apartamentos_p1 PARTITION OF apartamentos FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE apartamentos_p2 PARTITION OF apartamentos FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE apartamentos_p3 PARTITION OF apartamentos FOR VALUES WITH (MODULUS 4, REMAINDER 3);

CREATE INDEX idx_apartamentos_condominio_id ON apartamentos (condominio_id);
CREATE INDEX idx_apartamentos_bloco_id      ON apartamentos (bloco_id);

ALTER TABLE apartamentos
    ADD CONSTRAINT fk_apartamentos_condominio
    FOREIGN KEY (condominio_id) REFERENCES condominios (id);

ALTER TABLE apartamentos
    ADD CONSTRAINT fk_apartamentos_bloco
    FOREIGN KEY (bloco_id, condominio_id) REFERENCES blocos (id, condominio_id);

-- ── Step 6: Restore data ───────────────────────────────────────────────────
INSERT INTO blocos (id, condominio_id, numero, bloco, deleted, created_at, updated_at, created_by, updated_by)
    SELECT id, condominio_id, numero, bloco, deleted, created_at, updated_at, created_by, updated_by
    FROM blocos_bak;

INSERT INTO apartamentos (id, condominio_id, bloco_id, numero, deleted, created_at, updated_at, created_by, updated_by)
    SELECT id, condominio_id, bloco_id, numero, deleted, created_at, updated_at, created_by, updated_by
    FROM apartamentos_bak;

-- rollback DROP TABLE apartamentos CASCADE; DROP TABLE blocos CASCADE;
