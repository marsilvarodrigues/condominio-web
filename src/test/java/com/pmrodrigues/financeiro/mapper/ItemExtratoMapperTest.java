package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ItemExtratoMapperImpl.class)
class ItemExtratoMapperTest {

    @Autowired ItemExtratoMapper mapper;

    private PlanoContas planoContas(String codigo, String descricao) {
        return PlanoContas.builder().id(1L).codigo(codigo).descricao(descricao).tipo(TipoConta.RECEITA).build();
    }

    private ItemOrcamento itemOrcamento(Long id, PlanoContas pc) {
        return ItemOrcamento.builder().id(id).planoContas(pc)
                .valorPrevisto(BigDecimal.valueOf(1000)).valorRealizado(BigDecimal.valueOf(200)).build();
    }

    private ItemExtrato itemExtrato(Long id, TipoLancamento tipo, BigDecimal valor, String descricao, ItemOrcamento orcamento) {
        var e = ItemExtrato.builder().id(id).dataLancamento(LocalDate.of(2026, 5, 10))
                .valor(valor).tipo(tipo).descricao(descricao).status(StatusItemExtrato.PENDENTE).build();
        e.setItemOrcamento(orcamento);
        return e;
    }

    // ── toDTO ─────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllScalarFields() {
        var extrato = itemExtrato(42L, TipoLancamento.CREDITO, BigDecimal.valueOf(500), "Taxa cond.", null);

        var dto = mapper.toDTO(extrato);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.dataLancamento()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(dto.valor()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(dto.tipo()).isEqualTo(TipoLancamento.CREDITO);
        assertThat(dto.descricao()).isEqualTo("Taxa cond.");
        assertThat(dto.status()).isEqualTo(StatusItemExtrato.PENDENTE);
    }

    @Test
    void toDTO_whenItemOrcamentoIsNull_itemOrcamentoIsNull() {
        var extrato = itemExtrato(1L, TipoLancamento.DEBITO, BigDecimal.TEN, "Água", null);

        var dto = mapper.toDTO(extrato);

        assertThat(dto.itemOrcamento()).isNull();
    }

    @Test
    void toDTO_whenItemOrcamentoPresent_mapsNestedFields() {
        var pc = planoContas("1.2", "Taxa condominial");
        var orcamento = itemOrcamento(10L, pc);
        var extrato = itemExtrato(20L, TipoLancamento.CREDITO, BigDecimal.valueOf(300), "Taxa", orcamento);

        var dto = mapper.toDTO(extrato);

        assertThat(dto.itemOrcamento()).isNotNull();
        assertThat(dto.itemOrcamento().id()).isEqualTo(10L);
        assertThat(dto.itemOrcamento().codigoPlanoContas()).isEqualTo("1.2");
        assertThat(dto.itemOrcamento().descricaoPlanoContas()).isEqualTo("Taxa condominial");
        assertThat(dto.itemOrcamento().valorPrevisto()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(dto.itemOrcamento().valorRealizado()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    // ── toResumoResponse ──────────────────────────────────────────────────

    @Test
    void toResumoResponse_mapsAllFields() {
        var pc = planoContas("2.3", "Manutenção");
        var orcamento = itemOrcamento(5L, pc);

        var dto = mapper.toResumoResponse(orcamento);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.codigoPlanoContas()).isEqualTo("2.3");
        assertThat(dto.descricaoPlanoContas()).isEqualTo("Manutenção");
        assertThat(dto.valorPrevisto()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(dto.valorRealizado()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void toResumoResponse_whenNull_returnsNull() {
        assertThat(mapper.toResumoResponse(null)).isNull();
    }
}
