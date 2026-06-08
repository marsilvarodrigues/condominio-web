package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for updating mutable fields of a {@link
 * com.pmrodrigues.financeiro.model.LancamentoBancario}. Tipo, origem, and conta bancaria cannot be
 * changed after creation.
 */
public record UpdateLancamentoBancarioDTO(
    @NotNull LocalDate dataLancamento,
    @NotNull @Positive BigDecimal valor,
    @NotBlank String descricao,
    Long referenciaId) {}
