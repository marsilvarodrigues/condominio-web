package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for proportional rateio strategies (FRACAO_IDEAL and METRAGEM).
 *
 * <p>Both strategies follow the same algorithm: each unit's quota is proportional to
 * its weight ({@code cota_i = total × (weight_i / Σweights)}). The rounding remainder
 * is assigned to the unit with the largest weight.
 */
abstract class AbstractProporcionalStrategy implements EstrategiaRateio {

    private final Function<CoeficienteRateio, BigDecimal> pesoPicker;

    /**
     * @param pesoPicker function that extracts the numeric weight from a {@link CoeficienteRateio}
     */
    protected AbstractProporcionalStrategy(Function<CoeficienteRateio, BigDecimal> pesoPicker) {
        this.pesoPicker = pesoPicker;
    }

    @Override
    public Map<Long, BigDecimal> calcular(
            List<CoeficienteRateio> coeficientes,
            BigDecimal despesaTotal,
            Map<String, Object> parametros) {

        validarEntradas(coeficientes, despesaTotal);
        coeficientes.forEach(c -> {
            BigDecimal peso = pesoPicker.apply(c);
            if (peso == null) {
                throw new IllegalArgumentException(
                        "Peso nulo para apartamento id=" + c.getApartamento().getId());
            }
        });

        BigDecimal somaTotal = coeficientes.stream()
                .map(pesoPicker)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> cotas = new LinkedHashMap<>();
        BigDecimal somaCotasCalculadas = BigDecimal.ZERO;

        // sort deterministically so the residue always goes to the same unit
        List<CoeficienteRateio> ordenados = coeficientes.stream()
                .sorted(Comparator.comparing(pesoPicker, Comparator.reverseOrder()))
                .toList();

        for (int i = 0; i < ordenados.size() - 1; i++) {
            CoeficienteRateio c = ordenados.get(i);
            BigDecimal peso = pesoPicker.apply(c);
            BigDecimal cota = despesaTotal
                    .multiply(peso)
                    .divide(somaTotal, 2, RoundingMode.DOWN);
            cotas.put(c.getApartamento().getId(), cota);
            somaCotasCalculadas = somaCotasCalculadas.add(cota);
        }

        // last unit absorbs the rounding remainder
        CoeficienteRateio ultimo = ordenados.get(ordenados.size() - 1);
        cotas.put(ultimo.getApartamento().getId(),
                despesaTotal.subtract(somaCotasCalculadas));

        return cotas;
    }

    static void validarEntradas(List<CoeficienteRateio> coeficientes, BigDecimal despesaTotal) {
        if (coeficientes == null || coeficientes.isEmpty()) {
            throw new IllegalArgumentException("Lista de coeficientes não pode ser vazia");
        }
        if (despesaTotal == null || despesaTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("despesaTotal deve ser positivo");
        }
    }
}
