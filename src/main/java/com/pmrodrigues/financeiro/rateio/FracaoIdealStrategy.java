package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.TipoRateio;
import org.springframework.stereotype.Component;

/**
 * Splits the expense proportionally to each unit's ideal fraction coefficient.
 *
 * <p>Uses {@link CoeficienteRateio#getCoeficiente()} as the weight. The sum of coefficients is
 * never assumed to equal 1.0, since convention documents can carry rounding imprecision. The
 * rounding remainder is assigned to the unit with the largest coefficient.
 */
@Component("FRACAO_IDEAL")
public class FracaoIdealStrategy extends AbstractProporcionalStrategy {

  public FracaoIdealStrategy() {
    super(c -> c.getCoeficiente());
  }

  @Override
  public TipoRateio tipoRateio() {
    return TipoRateio.FRACAO_IDEAL;
  }
}
