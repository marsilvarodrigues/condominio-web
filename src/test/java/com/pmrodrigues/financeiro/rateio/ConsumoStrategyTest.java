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

class ConsumoStrategyTest {

    private ConsumoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ConsumoStrategy();
    }

    @Test
    void calcular_30PercentFixo70PercentVariavel() {
        // 2 units; consumo: 15m3 and 5m3
        // despFixa = 1000 * 0.30 = 300 → 150 each
        // despVar  = 1000 * 0.70 = 700 → 15/20 * 700 = 525 and 5/20 * 700 = 175
        // unit1 = 150 + 525 = 675; unit2 = 150 + 175 = 325
        var coefs = List.of(consumo(1L, "15.000"), consumo(2L, "5.000"));
        var params = Map.<String, Object>of("percentualFixo", new BigDecimal("0.30"));
        var cotas = strategy.calcular(coefs, new BigDecimal("1000.00"), params);

        assertThat(cotas.get(1L)).isEqualByComparingTo("675.00");
        assertThat(cotas.get(2L)).isEqualByComparingTo("325.00");
        assertSomaTotalEquals(cotas, new BigDecimal("1000.00"));
    }

    @Test
    void calcular_consumoZero_rateiaVariavelIgualmente() {
        var coefs = List.of(consumo(1L, "0.000"), consumo(2L, "0.000"));
        var params = Map.<String, Object>of("percentualFixo", new BigDecimal("0.50"));
        var cotas = strategy.calcular(coefs, new BigDecimal("100.00"), params);

        assertSomaTotalEquals(cotas, new BigDecimal("100.00"));
        // both units should be equal (50 each)
        assertThat(cotas.get(1L)).isEqualByComparingTo("50.00");
    }

    @Test
    void calcular_percentualFixoAusente_lancaExcecao() {
        var coefs = List.of(consumo(1L, "10.000"));
        assertThatThrownBy(() -> strategy.calcular(coefs, new BigDecimal("100.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percentualFixo");
    }

    @Test
    void calcular_percentualFixoForaDe0e1_lancaExcecao() {
        var coefs = List.of(consumo(1L, "10.000"));
        var params = Map.<String, Object>of("percentualFixo", new BigDecimal("1.5"));
        assertThatThrownBy(() -> strategy.calcular(coefs, new BigDecimal("100.00"), params))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calcular_listaVazia_lancaExcecao() {
        var params = Map.<String, Object>of("percentualFixo", new BigDecimal("0.30"));
        assertThatThrownBy(() -> strategy.calcular(List.of(), new BigDecimal("100.00"), params))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calcular_somaTotalSempre100() {
        var coefs = List.of(consumo(1L, "7.500"), consumo(2L, "3.300"), consumo(3L, "1.200"));
        var params = Map.<String, Object>of("percentualFixo", new BigDecimal("0.40"));
        var cotas = strategy.calcular(coefs, new BigDecimal("100.00"), params);
        assertSomaTotalEquals(cotas, new BigDecimal("100.00"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CoeficienteRateio consumo(Long id, String consumoM3) {
        var apt = new Apartamento();
        apt.setId(id);
        return CoeficienteRateio.builder()
                .apartamento(apt)
                .consumoM3(new BigDecimal(consumoM3))
                .vigencia(LocalDate.now())
                .build();
    }

    private void assertSomaTotalEquals(Map<Long, BigDecimal> cotas, BigDecimal expected) {
        BigDecimal soma = cotas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(expected);
    }
}
