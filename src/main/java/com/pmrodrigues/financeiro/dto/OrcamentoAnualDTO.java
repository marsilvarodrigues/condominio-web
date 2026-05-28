package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusOrcamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full representation of an OrcamentoAnual including its items, used for updates and responses.
 */
public record OrcamentoAnualDTO(
        Long id,
        @NotNull @Positive Integer exercicio,
        StatusOrcamento status,
        BigDecimal taxaEstimadaUnidade,
        List<ItemOrcamentoDTO> itens,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
