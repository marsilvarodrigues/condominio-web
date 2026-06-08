package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Full representation of a {@code GrupoDespesa} returned by read operations. */
public record GrupoDespesaDTO(
    Long id,
    @NotBlank String nome,
    @NotNull TipoRateio tipoRateio,
    @NotNull EscopoRateio escopo,
    Long blocoId,
    Long planoContasId,
    String parametrosJson) {}
