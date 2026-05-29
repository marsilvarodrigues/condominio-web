package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for creating a new {@link com.pmrodrigues.financeiro.model.LancamentoBancario}.
 */
public record CreateLancamentoBancarioDTO(
        @NotNull Long contaBancariaId,
        @NotNull LocalDate dataLancamento,
        @NotNull @Positive BigDecimal valor,
        @NotNull TipoLancamento tipo,
        @NotBlank String descricao,
        @NotNull OrigemLancamento origem,
        Long referenciaId
) {}
