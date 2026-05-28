package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for crediting an amount to the FundoReserva.
 */
public record CreditarFundoDTO(
        @NotNull @Positive BigDecimal valor,
        String justificativa,
        LocalDate dataMovimentacao
) {
}
