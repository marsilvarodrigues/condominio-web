package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoConta;

import java.math.BigDecimal;

/**
 * Response representation of a single budget line item.
 */
public record ItemOrcamentoDTO(
        Long id,
        Long planoContasId,
        String planoContasDescricao,
        TipoConta tipoConta,
        BigDecimal valorPrevisto,
        BigDecimal valorRealizado
) {
}
