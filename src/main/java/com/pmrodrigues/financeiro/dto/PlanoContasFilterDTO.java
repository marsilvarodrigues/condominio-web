package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoConta;

/**
 * Filter parameters for querying PlanoContas; no validation annotations (filters are always
 * optional).
 */
public record PlanoContasFilterDTO(TipoConta tipo, Long paiId) {}
