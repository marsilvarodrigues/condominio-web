package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.StatusExecucaoRateio;
import com.pmrodrigues.financeiro.model.TipoExecucaoRateio;

import java.time.LocalDateTime;

/**
 * Filter parameters for listing {@code RateioExecucao} records.
 */
public record RateioExecucaoFilterDTO(
        StatusExecucaoRateio status,
        TipoExecucaoRateio tipoExecucao,
        LocalDateTime dataInicio,
        LocalDateTime dataFim
) {}
