package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;

/**
 * Summary of the realisation of a budget line item computed from its associated statement entries.
 */
public record ContribuicaoResponse(
    Long itemOrcamentoId,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal percentualRealizado,
    int totalItensAssociados) {}
