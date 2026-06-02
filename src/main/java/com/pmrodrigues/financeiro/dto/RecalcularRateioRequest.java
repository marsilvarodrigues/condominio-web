package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * Request body for the forced recalculation endpoint.
 *
 * <p>The {@code confirmar} flag must be {@code true}; its presence as an explicit
 * field prevents accidental triggers and serves as an acknowledgement that all
 * previously calculated quotas will be replaced.
 */
public record RecalcularRateioRequest(
        @AssertTrue(message = "confirmar deve ser true para executar o recálculo") boolean confirmar
) {}
