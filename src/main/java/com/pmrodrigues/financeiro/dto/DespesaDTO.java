package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusRateio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Full representation of a {@code Despesa} returned by read operations. */
public record DespesaDTO(
    Long id,
    Long grupoDespesaId,
    String grupoDespesaNome,
    @NotBlank String descricao,
    @NotNull @Positive BigDecimal valorTotal,
    @NotNull LocalDate competencia,
    StatusRateio rateioStatus,
    LocalDateTime dataUltimoRateio) {}
