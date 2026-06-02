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

class IgualitarioStrategyTest {

    private IgualitarioStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IgualitarioStrategy();
    }

    @Test
    void calcular_distribuiIgualmente() {
        var coefs = coeficientes(1L, 2L, 3L);
        var cotas = strategy.calcular(coefs, new BigDecimal("300.00"), Map.of());

        assertThat(cotas).hasSize(3);
        assertThat(cotas.values()).allMatch(v -> v.compareTo(new BigDecimal("100.00")) == 0);
        assertSomaTotalEquals(cotas, new BigDecimal("300.00"));
    }

    @Test
    void calcular_arredondamento_centavoVaiParaPrimeiraUnidade() {
        // R$100 / 3 = 33.33; residuo = R$0.01 → unidade 1L recebe 33.34
        var coefs = coeficientes(1L, 2L, 3L);
        var cotas = strategy.calcular(coefs, new BigDecimal("100.00"), Map.of());

        assertThat(cotas.get(1L)).isEqualByComparingTo("33.34");
        assertThat(cotas.get(2L)).isEqualByComparingTo("33.33");
        assertThat(cotas.get(3L)).isEqualByComparingTo("33.33");
        assertSomaTotalEquals(cotas, new BigDecimal("100.00"));
    }

    @Test
    void calcular_umaUnidade_recebeTudo() {
        var coefs = coeficientes(42L);
        var cotas = strategy.calcular(coefs, new BigDecimal("500.50"), Map.of());

        assertThat(cotas.get(42L)).isEqualByComparingTo("500.50");
    }

    @Test
    void calcular_listaVazia_lancaExcecao() {
        assertThatThrownBy(() -> strategy.calcular(List.of(), new BigDecimal("100.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    void calcular_totalZero_lancaExcecao() {
        assertThatThrownBy(() -> strategy.calcular(coeficientes(1L), BigDecimal.ZERO, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calcular_totalNegativo_lancaExcecao() {
        assertThatThrownBy(() -> strategy.calcular(coeficientes(1L), new BigDecimal("-1.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<CoeficienteRateio> coeficientes(Long... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> {
                    var apt = new Apartamento();
                    apt.setId(id);
                    return CoeficienteRateio.builder()
                            .apartamento(apt)
                            .coeficiente(BigDecimal.ONE)
                            .vigencia(LocalDate.now())
                            .build();
                }).toList();
    }

    private void assertSomaTotalEquals(Map<Long, BigDecimal> cotas, BigDecimal expected) {
        BigDecimal soma = cotas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(expected);
    }
}
