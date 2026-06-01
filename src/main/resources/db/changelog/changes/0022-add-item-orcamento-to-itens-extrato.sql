-- liquibase formatted sql

-- changeset pmrodrigues:0022-add-item-orcamento-to-itens-extrato dbms:postgresql

ALTER TABLE itens_extrato ADD COLUMN item_orcamento_id BIGINT;

ALTER TABLE itens_extrato
    ADD CONSTRAINT fk_itens_extrato_item_orcamento
    FOREIGN KEY (item_orcamento_id, condominio_id) REFERENCES item_orcamento(id, condominio_id);

CREATE INDEX idx_itens_extrato_item_orcamento_id
    ON itens_extrato (item_orcamento_id)
    WHERE item_orcamento_id IS NOT NULL;

CREATE INDEX idx_itens_extrato_item_orcamento_condominio
    ON itens_extrato (condominio_id, item_orcamento_id)
    WHERE item_orcamento_id IS NOT NULL;

-- rollback DROP INDEX IF EXISTS idx_itens_extrato_item_orcamento_condominio; DROP INDEX IF EXISTS idx_itens_extrato_item_orcamento_id; ALTER TABLE itens_extrato DROP CONSTRAINT IF EXISTS fk_itens_extrato_item_orcamento; ALTER TABLE itens_extrato DROP COLUMN IF EXISTS item_orcamento_id;
