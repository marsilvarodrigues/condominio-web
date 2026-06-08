package com.pmrodrigues.financeiro.dto;

import java.math.BigDecimal;

/**
 * Represents the calculated quota for a single apartment unit in a rateio simulation.
 *
 * @param apartamentoId the unit identifier
 * @param unidade the unit number/label
 * @param peso the raw weight used (coefficient, area, or consumption)
 * @param percentualPeso the unit's weight as a percentage of the total weight
 * @param cota the calculated monetary quota
 * @param percentualCota the unit's quota as a percentage of the expense total
 */
public record CotaUnidadeDTO(
    Long apartamentoId,
    String unidade,
    BigDecimal peso,
    BigDecimal percentualPeso,
    BigDecimal cota,
    BigDecimal percentualCota) {}
