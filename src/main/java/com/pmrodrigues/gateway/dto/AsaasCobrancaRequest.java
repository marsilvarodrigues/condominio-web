package com.pmrodrigues.gateway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body sent to {@code POST /payments} on the Asaas API.
 *
 * @param customer Asaas customer ID (e.g. {@code cus_000123456789})
 * @param value charge amount in BRL
 * @param dueDate payment due date
 * @param billingType payment method, e.g. {@code "BOLETO_PIX"}
 * @param description charge description displayed to the payer
 * @param fine fine configuration applied when overdue
 * @param interest monthly interest configuration applied when overdue
 */
public record AsaasCobrancaRequest(
    String customer,
    BigDecimal value,
    LocalDate dueDate,
    String billingType,
    String description,
    AsaasFineRequest fine,
    AsaasInterestRequest interest) {}
