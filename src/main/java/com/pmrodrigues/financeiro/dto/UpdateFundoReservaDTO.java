package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Payload for updating the percentual and bank account of an existing FundoReserva. */
public record UpdateFundoReservaDTO(
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentualArrecadacao,
    Long contaBancariaId) {}
