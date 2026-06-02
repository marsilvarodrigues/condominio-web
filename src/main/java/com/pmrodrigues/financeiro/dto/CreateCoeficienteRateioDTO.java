package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating or updating a coefficient record for a unit within a group.
 */
public record CreateCoeficienteRateioDTO(
        @NotNull Long apartamentoId,
        BigDecimal coeficiente,
        BigDecimal areaM2,
        BigDecimal consumoM3,
        @NotNull LocalDate vigencia
) {}
