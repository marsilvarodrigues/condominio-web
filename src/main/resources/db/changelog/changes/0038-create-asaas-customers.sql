-- liquibase formatted sql
-- changeset pmrodrigues:0038-create-asaas-customers dbms:postgresql

CREATE TABLE asaas_customers (
    id          BIGSERIAL    PRIMARY KEY,
    pessoa_id   BIGINT       NOT NULL UNIQUE,
    customer_id VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_asaas_customers_pessoa ON asaas_customers (pessoa_id);
