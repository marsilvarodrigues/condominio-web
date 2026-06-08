package com.pmrodrigues.gateway.dto;

/**
 * Request body sent to {@code POST /customers} on the Asaas API.
 *
 * @param name full name of the customer
 * @param cpfCnpj Brazilian CPF (individuals) or CNPJ (companies) — digits only
 * @param email contact email address (optional but strongly recommended)
 * @param mobilePhone mobile phone number (optional)
 */
public record AsaasCustomerRequest(String name, String cpfCnpj, String email, String mobilePhone) {}
