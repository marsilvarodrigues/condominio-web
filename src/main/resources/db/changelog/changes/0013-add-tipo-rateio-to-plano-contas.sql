-- liquibase formatted sql

-- changeset pmrodrigues:0013-add-tipo-rateio-to-plano-contas

ALTER TABLE plano_contas
    ADD COLUMN tipo_rateio VARCHAR(20)
        CHECK (tipo_rateio IN ('IGUALITARIO','FRACAO_IDEAL','TODOS','POR_BLOCO'));

-- rollback ALTER TABLE plano_contas DROP COLUMN tipo_rateio;
