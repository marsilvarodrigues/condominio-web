package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for debiting an amount from the FundoReserva; justification is mandatory. */
public record DebitarFundoDTO(
    @NotNull @Positive BigDecimal valor,
    @NotBlank String justificativa,
    LocalDate dataMovimentacao) {}
