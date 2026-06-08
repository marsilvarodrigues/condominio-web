package com.pmrodrigues.cobranca.dto;

import com.pmrodrigues.cobranca.model.StatusCobranca;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight summary of a {@code Cobranca}, used in apartment-scoped listing endpoints.
 *
 * @param id charge primary key
 * @param vencimento due date
 * @param valor charge amount in BRL
 * @param status current lifecycle status
 * @param criadaEm creation timestamp
 * @param pagoEm payment confirmation timestamp (may be null)
 * @param emailEnviado whether the billing email was dispatched
 */
public record CobrancaResumoDTO(
    Long id,
    LocalDate vencimento,
    BigDecimal valor,
    StatusCobranca status,
    LocalDateTime criadaEm,
    LocalDateTime pagoEm,
    boolean emailEnviado) {}
