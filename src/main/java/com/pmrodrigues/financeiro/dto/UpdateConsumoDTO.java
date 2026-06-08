package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Payload for updating the monthly water/gas consumption reading of a coefficient record. */
public record UpdateConsumoDTO(@NotNull BigDecimal consumoM3) {}
