package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload to remove the association between an
 * {@link com.pmrodrigues.financeiro.model.ItemExtrato} and its current
 * {@link com.pmrodrigues.financeiro.model.ItemOrcamento}.
 */
public record DesassociarItemOrcamentoRequest(
        @NotBlank @Size(max = 500) String justificativa
) {}
