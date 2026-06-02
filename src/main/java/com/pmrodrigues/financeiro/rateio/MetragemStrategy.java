package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Splits the expense proportionally to each unit's floor area (m²).
 *
 * <p>Uses {@link CoeficienteRateio#getAreaM2()} as the weight. Throws
 * {@link IllegalArgumentException} if any unit has a null {@code areaM2}.
 * The rounding remainder goes to the unit with the largest area.
 */
@Component("METRAGEM")
public class MetragemStrategy extends AbstractProporcionalStrategy {

    public MetragemStrategy() {
        super(c -> c.getAreaM2());
    }

    @Override
    public Map<Long, BigDecimal> calcular(
            List<CoeficienteRateio> coeficientes,
            BigDecimal despesaTotal,
            Map<String, Object> parametros) {

        coeficientes.forEach(c -> {
            if (c.getAreaM2() == null) {
                throw new IllegalArgumentException(
                        "areaM2 não pode ser nulo para apartamento id=" + c.getApartamento().getId());
            }
        });
        return super.calcular(coeficientes, despesaTotal, parametros);
    }

    @Override
    public TipoRateio tipoRateio() {
        return TipoRateio.METRAGEM;
    }
}
