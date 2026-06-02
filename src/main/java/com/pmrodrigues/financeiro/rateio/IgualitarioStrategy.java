package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits the expense equally among all units.
 *
 * <p>Formula: {@code cota = despesaTotal / n}. The rounding remainder (at most R$0.01)
 * is assigned to the first unit in the list (index 0) as a deterministic tiebreaker.
 */
@Component("IGUALITARIO")
public class IgualitarioStrategy implements EstrategiaRateio {

    @Override
    public Map<Long, BigDecimal> calcular(
            List<CoeficienteRateio> coeficientes,
            BigDecimal despesaTotal,
            Map<String, Object> parametros) {

        AbstractProporcionalStrategy.validarEntradas(coeficientes, despesaTotal);

        int n = coeficientes.size();
        BigDecimal cotaBase = despesaTotal.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

        Map<Long, BigDecimal> cotas = new LinkedHashMap<>();
        BigDecimal somaCotasBase = cotaBase.multiply(BigDecimal.valueOf(n));
        BigDecimal residuo = despesaTotal.subtract(somaCotasBase);

        for (int i = 0; i < coeficientes.size(); i++) {
            BigDecimal valor = i == 0 ? cotaBase.add(residuo) : cotaBase;
            cotas.put(coeficientes.get(i).getApartamento().getId(), valor);
        }

        return cotas;
    }

    @Override
    public TipoRateio tipoRateio() {
        return TipoRateio.IGUALITARIO;
    }
}
