package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;

/**
 * Filter parameters for listing {@code GrupoDespesa} records.
 */
public record GrupoDespesaFilterDTO(
        TipoRateio tipoRateio,
        EscopoRateio escopo
) {}
