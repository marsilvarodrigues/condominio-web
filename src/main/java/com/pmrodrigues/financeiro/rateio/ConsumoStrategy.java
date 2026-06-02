package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import com.pmrodrigues.financeiro.model.TipoRateio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits the expense into a fixed portion (equally divided) and a variable portion (by consumption).
 *
 * <p>Formula:
 * <pre>
 *   despFixa     = despesaTotal × percentualFixo
 *   despVariavel = despesaTotal × (1 - percentualFixo)
 *   cotaFixa_i   = despFixa / n
 *   cotaVar_i    = despVariavel × (consumoM3_i / Σconsumos)
 *   cota_i       = cotaFixa_i + cotaVar_i
 * </pre>
 *
 * <p>If {@code Σconsumos == 0}, the variable portion is split equally and a warning is logged.
 * The rounding remainder is assigned to the unit with the highest consumption.
 *
 * @param parametros must contain {@code "percentualFixo"} as a {@link BigDecimal} in [0, 1]
 */
@Slf4j
@Component("CONSUMO")
public class ConsumoStrategy implements EstrategiaRateio {

    private static final String PARAM_PERCENTUAL_FIXO = "percentualFixo";

    @Override
    public Map<Long, BigDecimal> calcular(
            List<CoeficienteRateio> coeficientes,
            BigDecimal despesaTotal,
            Map<String, Object> parametros) {

        AbstractProporcionalStrategy.validarEntradas(coeficientes, despesaTotal);

        BigDecimal percentualFixo = extrairPercentualFixo(parametros);

        int n = coeficientes.size();
        BigDecimal despFixa = despesaTotal.multiply(percentualFixo).setScale(2, RoundingMode.DOWN);
        BigDecimal despVariavel = despesaTotal.subtract(despFixa);

        BigDecimal cotaFixaBase = despFixa.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal residuoFixo = despFixa.subtract(cotaFixaBase.multiply(BigDecimal.valueOf(n)));

        BigDecimal somaConsumos = coeficientes.stream()
                .map(c -> c.getConsumoM3() != null ? c.getConsumoM3() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean consumoZero = somaConsumos.compareTo(BigDecimal.ZERO) == 0;
        if (consumoZero) {
            log.warn("ConsumoStrategy: Σconsumos == 0, rateando parte variável igualmente");
        }

        // sort by consumption desc for deterministic residue assignment
        List<CoeficienteRateio> ordenados = coeficientes.stream()
                .sorted(Comparator.comparing(
                        c -> c.getConsumoM3() != null ? c.getConsumoM3() : BigDecimal.ZERO,
                        Comparator.reverseOrder()))
                .toList();

        Map<Long, BigDecimal> cotasVariaveis = calcularVariavel(
                ordenados, despVariavel, somaConsumos, consumoZero);

        Map<Long, BigDecimal> cotas = new LinkedHashMap<>();
        BigDecimal somaCotaFixa = BigDecimal.ZERO;

        for (int i = 0; i < ordenados.size(); i++) {
            CoeficienteRateio c = ordenados.get(i);
            BigDecimal cotaFixa = i == 0 ? cotaFixaBase.add(residuoFixo) : cotaFixaBase;
            somaCotaFixa = somaCotaFixa.add(cotaFixa);
            BigDecimal cotaVar = cotasVariaveis.getOrDefault(c.getApartamento().getId(), BigDecimal.ZERO);
            cotas.put(c.getApartamento().getId(), cotaFixa.add(cotaVar));
        }

        return cotas;
    }

    private Map<Long, BigDecimal> calcularVariavel(
            List<CoeficienteRateio> ordenados,
            BigDecimal despVariavel,
            BigDecimal somaConsumos,
            boolean consumoZero) {

        int n = ordenados.size();
        Map<Long, BigDecimal> cotasVar = new LinkedHashMap<>();
        BigDecimal somaVar = BigDecimal.ZERO;

        for (int i = 0; i < ordenados.size() - 1; i++) {
            CoeficienteRateio c = ordenados.get(i);
            BigDecimal cota;
            if (consumoZero) {
                cota = despVariavel.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
            } else {
                BigDecimal consumo = c.getConsumoM3() != null ? c.getConsumoM3() : BigDecimal.ZERO;
                cota = despVariavel.multiply(consumo).divide(somaConsumos, 2, RoundingMode.DOWN);
            }
            cotasVar.put(c.getApartamento().getId(), cota);
            somaVar = somaVar.add(cota);
        }

        // last unit absorbs variable residue
        CoeficienteRateio ultimo = ordenados.get(ordenados.size() - 1);
        cotasVar.put(ultimo.getApartamento().getId(), despVariavel.subtract(somaVar));

        return cotasVar;
    }

    private BigDecimal extrairPercentualFixo(Map<String, Object> parametros) {
        Object valor = parametros == null ? null : parametros.get(PARAM_PERCENTUAL_FIXO);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Parâmetro obrigatório ausente: " + PARAM_PERCENTUAL_FIXO);
        }
        BigDecimal pf;
        if (valor instanceof BigDecimal bd) {
            pf = bd;
        } else if (valor instanceof String s) {
            try {
                pf = new BigDecimal(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Parâmetro inválido: " + PARAM_PERCENTUAL_FIXO + " = " + valor);
            }
        } else {
            pf = new BigDecimal(valor.toString());
        }
        if (pf.compareTo(BigDecimal.ZERO) < 0 || pf.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    PARAM_PERCENTUAL_FIXO + " deve estar em [0, 1], valor=" + pf);
        }
        return pf;
    }

    @Override
    public TipoRateio tipoRateio() {
        return TipoRateio.CONSUMO;
    }
}
