package com.pmrodrigues.gateway.dto;

/**
 * Subset of the Asaas {@code Customer} object returned by {@code POST /customers}.
 *
 * @param id Asaas customer ID (e.g. {@code cus_000123456789})
 * @param name customer name as stored in Asaas
 * @param cpfCnpj CPF or CNPJ registered in Asaas
 */
public record AsaasCustomerResponse(String id, String name, String cpfCnpj) {}
