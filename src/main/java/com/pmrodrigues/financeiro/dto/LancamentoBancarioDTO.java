package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only projection of a {@link com.pmrodrigues.financeiro.model.LancamentoBancario}.
 */
public record LancamentoBancarioDTO(
        Long id,
        Long contaBancariaId,
        String contaBancariaDescricao,
        LocalDate dataLancamento,
        BigDecimal valor,
        TipoLancamento tipo,
        String descricao,
        OrigemLancamento origem,
        Long referenciaId,
        StatusLancamento status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
