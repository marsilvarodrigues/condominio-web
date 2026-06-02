package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request body for a non-persisted rateio simulation.
 */
public record SimularRateioRequest(
        @NotNull Long grupoDespesaId,
        @NotNull @Positive BigDecimal despesaTotal,
        Map<String, Object> parametros
) {}
