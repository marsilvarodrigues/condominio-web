package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoMovimentacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Response representation of a single FundoReserva movement (credit or debit). */
public record FundoReservaMovimentacaoDTO(
    Long id,
    TipoMovimentacao tipo,
    BigDecimal valor,
    String justificativa,
    LocalDate dataMovimentacao,
    LocalDateTime createdAt) {}
