package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload to associate an {@link com.pmrodrigues.financeiro.model.ItemExtrato} with an
 * {@link com.pmrodrigues.financeiro.model.ItemOrcamento}.
 */
public record AssociarItemOrcamentoRequest(@NotNull Long itemOrcamentoId) {}
