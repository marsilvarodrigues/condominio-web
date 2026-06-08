package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusExecucaoRateio;
import com.pmrodrigues.financeiro.model.TipoExecucaoRateio;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representation of a {@code RateioExecucao} audit record. */
public record RateioExecucaoDTO(
    Long id,
    Long despesaId,
    String despesaDescricao,
    Long grupoDespesaId,
    TipoExecucaoRateio tipoExecucao,
    LocalDateTime dataExecucao,
    BigDecimal despesaTotal,
    Integer totalUnidades,
    BigDecimal totalCotas,
    StatusExecucaoRateio status,
    String erroMensagem) {}
