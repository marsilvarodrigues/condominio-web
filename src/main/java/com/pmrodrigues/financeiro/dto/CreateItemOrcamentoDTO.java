package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Payload for adding a budget line item to an existing OrcamentoAnual. */
public record CreateItemOrcamentoDTO(
    @NotNull Long planoContasId, @NotNull @Positive BigDecimal valorPrevisto) {}
