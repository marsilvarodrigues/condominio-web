package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response representation of a FundoReserva.
 */
public record FundoReservaDTO(
        Long id,
        BigDecimal percentualArrecadacao,
        BigDecimal saldoAtual,
        String contaBancariaDestino,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
