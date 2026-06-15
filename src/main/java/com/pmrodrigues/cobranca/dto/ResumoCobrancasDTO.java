package com.pmrodrigues.cobranca.dto;

import java.math.BigDecimal;

/** Aggregate summary of pending and overdue charges for the current tenant. */
public record ResumoCobrancasDTO(
    long quantidadePendente,
    BigDecimal totalPendente,
    long quantidadeVencida,
    BigDecimal totalVencido) {}
