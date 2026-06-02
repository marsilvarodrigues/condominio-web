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

class MetragemStrategyTest {

    private MetragemStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MetragemStrategy();
    }

    @Test
    void calcular_proporcionalAArea() {
        var coefs = List.of(area(1L, "80.00"), area(2L, "60.00"), area(3L, "60.00"));
        var cotas = strategy.calcular(coefs, new BigDecimal("2000.00"), Map.of());

        // 80/200 * 2000 = 800, 60/200 * 2000 = 600
        assertThat(cotas.get(1L)).isEqualByComparingTo("800.00");
        assertSomaTotalEquals(cotas, new BigDecimal("2000.00"));
    }

    @Test
    void calcular_areaNula_lancaExcecao() {
        var coefs = List.of(area(1L, "80.00"), areaNull(2L));
        assertThatThrownBy(() -> strategy.calcular(coefs, new BigDecimal("100.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("areaM2");
    }

    @Test
    void calcular_listaVazia_lancaExcecao() {
        assertThatThrownBy(() -> strategy.calcular(List.of(), new BigDecimal("100.00"), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calcular_somaTotalCorreto() {
        var coefs = List.of(area(1L, "50.50"), area(2L, "49.50"));
        var cotas = strategy.calcular(coefs, new BigDecimal("1000.00"), Map.of());
        assertSomaTotalEquals(cotas, new BigDecimal("1000.00"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CoeficienteRateio area(Long id, String areaMq) {
        var apt = new Apartamento();
        apt.setId(id);
        return CoeficienteRateio.builder()
                .apartamento(apt)
                .areaM2(new BigDecimal(areaMq))
                .vigencia(LocalDate.now())
                .build();
    }

    private CoeficienteRateio areaNull(Long id) {
        var apt = new Apartamento();
        apt.setId(id);
        return CoeficienteRateio.builder()
                .apartamento(apt)
                .areaM2(null)
                .vigencia(LocalDate.now())
                .build();
    }

    private void assertSomaTotalEquals(Map<Long, BigDecimal> cotas, BigDecimal expected) {
        BigDecimal soma = cotas.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo(expected);
    }
}
