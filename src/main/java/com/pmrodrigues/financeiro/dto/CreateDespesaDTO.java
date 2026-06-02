package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating a new {@code Despesa}.
 */
public record CreateDespesaDTO(
        @NotNull Long grupoDespesaId,
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valorTotal,
        @NotNull LocalDate competencia
) {}
