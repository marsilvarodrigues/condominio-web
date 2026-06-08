package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoContaBancaria;

/**
 * Filter parameters for querying {@link com.pmrodrigues.financeiro.model.ContaBancaria} entries.
 * All fields are optional; absent values are ignored.
 */
public record ContaBancariaFilterDTO(
    TipoContaBancaria tipo, Boolean ativa, String agencia, String conta) {}
