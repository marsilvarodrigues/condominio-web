package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;

/**
 * Compact read-only summary of an {@link com.pmrodrigues.financeiro.model.ItemOrcamento}, embedded
 * inside responses that need a lightweight budget-line reference.
 */
public record ItemOrcamentoResumoResponse(
    Long id,
    String codigoPlanoContas,
    String descricaoPlanoContas,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado) {}
