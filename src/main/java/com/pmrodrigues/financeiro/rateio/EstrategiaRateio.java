package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for apportioning an expense total among condominium units.
 *
 * <p>Contract: {@code Σ(returned values) == despesaTotal} with tolerance ±R$0.01 due to rounding.
 * The rounding remainder is assigned to the unit with the largest weight, ensuring a deterministic
 * and exact result.
 *
 * @see IgualitarioStrategy
 * @see FracaoIdealStrategy
 * @see MetragemStrategy
 * @see ConsumoStrategy
 */
public interface EstrategiaRateio {

  /**
   * Calculates the quota for each unit.
   *
   * @param coeficientes the weight records for each unit in this group; must not be empty
   * @param despesaTotal the total expense amount to apportion; must be positive
   * @param parametros strategy-specific parameters; empty map for IGUALITARIO, FRACAO_IDEAL and
   *     METRAGEM; must contain {@code "percentualFixo"} (BigDecimal) for CONSUMO
   * @return a map from {@code apartamentoId} to the calculated quota
   * @throws IllegalArgumentException if coeficientes is empty, despesaTotal is not positive, or
   *     required parametros entries are missing/invalid
   */
  Map<Long, BigDecimal> calcular(
      List<CoeficienteRateio> coeficientes,
      BigDecimal despesaTotal,
      Map<String, Object> parametros);

  /** Returns the {@link TipoRateio} that this strategy implements. */
  TipoRateio tipoRateio();
}
