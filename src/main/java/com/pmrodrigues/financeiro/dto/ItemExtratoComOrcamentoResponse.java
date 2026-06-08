package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusItemExtrato;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only projection of an {@link com.pmrodrigues.financeiro.model.ItemExtrato} including its
 * optionally associated {@link com.pmrodrigues.financeiro.model.ItemOrcamento}.
 */
public record ItemExtratoComOrcamentoResponse(
    Long id,
    LocalDate dataLancamento,
    BigDecimal valor,
    TipoLancamento tipo,
    String descricao,
    StatusItemExtrato status,
    ItemOrcamentoResumoResponse itemOrcamento) {}
