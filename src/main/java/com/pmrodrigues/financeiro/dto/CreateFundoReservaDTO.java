package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for creating a new FundoReserva for the current condominium.
 */
public record CreateFundoReservaDTO(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentualArrecadacao,
        @Size(max = 100) String contaBancariaDestino
) {
}
