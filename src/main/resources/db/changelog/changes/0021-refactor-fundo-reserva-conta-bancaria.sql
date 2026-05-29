-- liquibase formatted sql

-- changeset pmrodrigues:0021-fundo-reserva-add-conta-bancaria-id
ALTER TABLE fundo_reserva ADD COLUMN conta_bancaria_id BIGINT;
ALTER TABLE fundo_reserva DROP COLUMN IF EXISTS conta_bancaria_destino;

-- rollback ALTER TABLE fundo_reserva ADD COLUMN conta_bancaria_destino VARCHAR(100); ALTER TABLE fundo_reserva DROP COLUMN IF EXISTS conta_bancaria_id;

-- changeset pmrodrigues:0021-fundo-reserva-fk-conta-bancaria dbms:postgresql
CREATE INDEX idx_fundo_reserva_conta_bancaria_id
    ON fundo_reserva (conta_bancaria_id)
    WHERE conta_bancaria_id IS NOT NULL;

ALTER TABLE fundo_reserva
    ADD CONSTRAINT fk_fundo_reserva_conta_bancaria
    FOREIGN KEY (conta_bancaria_id, condominio_id) REFERENCES contas_bancarias(id, condominio_id);

-- rollback ALTER TABLE fundo_reserva DROP CONSTRAINT IF EXISTS fk_fundo_reserva_conta_bancaria; DROP INDEX IF EXISTS idx_fundo_reserva_conta_bancaria_id;
