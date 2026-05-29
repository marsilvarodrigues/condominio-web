package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoContaBancaria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only representation of a {@link com.pmrodrigues.financeiro.model.ContaBancaria}.
 */
public record ContaBancariaDTO(
        Long id,
        Long bancoId,
        String bancoNome,
        String bancoCodigo,
        TipoContaBancaria tipo,
        String agencia,
        String conta,
        String digito,
        String descricao,
        String chavePix,
        BigDecimal saldoContabil,
        boolean ativa,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
