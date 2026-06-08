package com.pmrodrigues.cobranca.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request payload for the "generate charges" endpoint.
 *
 * @param execucaoId ID of the {@code RateioExecucao} whose {@code CotaRateio} records should
 *                   be converted into charges
 * @param vencimento target due date for all generated charges
 */
public record GerarCobrancasDTO(
        @NotNull Long execucaoId,
        @NotNull LocalDate vencimento
) {}
