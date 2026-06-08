package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of a non-persisted rateio simulation.
 *
 * @param cotas one entry per unit with quota details
 * @param somaCotas sum of all calculated quotas (should equal {@code despesaTotal})
 */
public record SimularRateioResponse(List<CotaUnidadeDTO> cotas, BigDecimal somaCotas) {}
