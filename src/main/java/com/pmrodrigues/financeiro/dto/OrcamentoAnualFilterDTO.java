package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusOrcamento;

/**
 * Filter parameters for querying OrcamentoAnual; all fields are optional.
 */
public record OrcamentoAnualFilterDTO(Integer exercicio, StatusOrcamento status) {
}
