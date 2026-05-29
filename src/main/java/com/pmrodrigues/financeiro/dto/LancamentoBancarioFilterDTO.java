package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;

import java.time.LocalDate;

/**
 * Filter parameters for querying {@link com.pmrodrigues.financeiro.model.LancamentoBancario} entries.
 * All fields are optional — null means no restriction.
 */
public record LancamentoBancarioFilterDTO(
        Long contaBancariaId,
        TipoLancamento tipo,
        OrigemLancamento origem,
        StatusLancamento status,
        LocalDate dataInicio,
        LocalDate dataFim
) {}
