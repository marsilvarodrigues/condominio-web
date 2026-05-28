-- liquibase formatted sql

-- changeset pmrodrigues:0014-add-escopo-rateio-to-plano-contas

-- Restrict tipo_rateio to the two apportionment methods (remove the scope values)
ALTER TABLE plano_contas DROP CONSTRAINT IF EXISTS plano_contas_tipo_rateio_check;
ALTER TABLE plano_contas ADD CONSTRAINT plano_contas_tipo_rateio_check
    CHECK (tipo_rateio IN ('IGUALITARIO','FRACAO_IDEAL','METRAGEM'));

-- New scope column: which units are included in the rateio calculation
ALTER TABLE plano_contas
    ADD COLUMN escopo_rateio VARCHAR(20)
        CONSTRAINT plano_contas_escopo_rateio_check CHECK (escopo_rateio IN ('TODOS','POR_BLOCO'));

-- rollback ALTER TABLE plano_contas DROP CONSTRAINT IF EXISTS plano_contas_escopo_rateio_check;
-- rollback ALTER TABLE plano_contas DROP COLUMN IF EXISTS escopo_rateio;
-- rollback ALTER TABLE plano_contas DROP CONSTRAINT IF EXISTS plano_contas_tipo_rateio_check;
-- rollback ALTER TABLE plano_contas ADD CONSTRAINT plano_contas_tipo_rateio_check CHECK (tipo_rateio IN ('IGUALITARIO','FRACAO_IDEAL','TODOS','POR_BLOCO'));
