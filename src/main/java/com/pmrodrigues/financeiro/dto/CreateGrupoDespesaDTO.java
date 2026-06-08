package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload for creating a new {@code GrupoDespesa}. */
public record CreateGrupoDespesaDTO(
    @NotBlank String nome,
    @NotNull TipoRateio tipoRateio,
    @NotNull EscopoRateio escopo,
    Long blocoId,
    Long planoContasId,
    String parametrosJson) {}
