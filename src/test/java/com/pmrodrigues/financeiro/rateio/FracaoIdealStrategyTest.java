package com.pmrodrigues.financeiro.rateio;

import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FracaoIdealStrategyTest {

    private FracaoIdealStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FracaoIdealStrategy();
    }

    @Test
    void calcular_proporcionalAoCoeficiente() {
        // coef: 0.5, 0.3, 0.2 — total 1.0
        var coefs = List.of(
                coef(1L, "0.500000"),
                coef(2L, "0.300000"),
                coef(3L, "0.200000"));

        var cotas = strategy.calcular(coefs, new BigDecimal("1000.00"), Map.of());

        assertThat(cotas.get(1L)).isEqualByComparingTo("500.00");
        assertThat(cotas.get(2L)).isEqualByComparingTo("300.00");
        assertThat(cotas.get(3L)).isEqualByComparingTo("200.00");
        assertSomaTotalEquals(cotas, new BigDecimal("1000.00"));
    }

    @Test
    void calcular_somaCoefNaoe1_funcionaCorretamente() {
        // conventions may sum to != 1.0 due to precision
        var coefs = List.of(coef(1L, "0.333"), coef(2L, "0.333"), coef(3L, "0.333"));
        var cotas = strategy.calcular(coefs, new BigDecimal("100.00"), Map.of());
        assertSomaTotalEquals(cotas, new BigDecimal("100.00"));
    }

    @Test
    void calcular_residuoVaiParaUnidadeMaiorCoeficiente() {
        // R$10 split with 0.7 and 0.3 → 7.00 and 3.00 exact
        var coefs = List.of(coef(1L, "0.7"), coef(2L, "0.3"));
        var cotas = strategy.calcular(coefs, new BigDecimal("10.00"), Map.of());
        assertSomaTotalEquals(cotas, new BigDecimal("10.00"));
    }

    @Test
    void calcular_listaVazia_lancaExcecao() {
        assertThatThrownBy(() -> strategy.calcular(List.of(), new BigDecimal("100.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calcular_totalNegativo_lancaExcecao() {
        assertThatThrownBy(() ->
                strategy.calcular(List.of(coef(1L, "1.0")), new BigDecimal("-1.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CoeficienteRateio coef(Long id, String coeficiente) {
        var apt = new Apartamento();
        apt.setId(id);
        return CoeficienteRateio.builder()
                .apartamento(apt)
                .coeficiente(new BigDecimal(coeficiente))
                .vigencia(LocalDate.now())
                .build();
    }

    private void assertSomaTotalEquals(Map<Long, BigDecimal> cotas, BigDecimal expected) {
        BigDecimal soma = cotas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(expected);
    }
}
