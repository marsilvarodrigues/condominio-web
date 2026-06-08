package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;

/**
 * A scored suggestion of an {@link com.pmrodrigues.financeiro.model.ItemOrcamento} that might match
 * a given {@link com.pmrodrigues.financeiro.model.ItemExtrato}.
 *
 * <p>{@code score} ranges 0–100; higher means a better fit.
 */
public record SugestaoItemOrcamentoResponse(
    Long itemOrcamentoId,
    String codigoPlanoContas,
    String descricaoPlanoContas,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    int score,
    boolean tipoCompativel,
    String motivoSugestao) {}
