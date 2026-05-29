package com.pmrodrigues.financeiro.mapper;

import com.pmrodrigues.financeiro.dto.CreateItemOrcamentoDTO;
import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.PlanoContas;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrcamentoAnualMapperImpl.class)
class OrcamentoAnualMapperTest {

    @Autowired OrcamentoAnualMapper mapper;
    @MockitoBean EntityManager em;

    // ── toItemEntity ──────────────────────────────────────────────────────

    @Test
    void toItemEntity_mapsOrcamentoAndPlanoContasProxy() {
        var orcamento = OrcamentoAnual.builder().id(1L).exercicio(2026)
                .status(StatusOrcamento.RASCUNHO).itens(List.of()).build();
        var planoContas = new PlanoContas();
        planoContas.setId(5L);
        when(em.getReference(PlanoContas.class, 5L)).thenReturn(planoContas);
        var dto = new CreateItemOrcamentoDTO(5L, new BigDecimal("500.00"));

        var item = mapper.toItemEntity(orcamento, dto);

        assertThat(item.getOrcamentoAnual()).isEqualTo(orcamento);
        assertThat(item.getPlanoContas()).isNotNull();
        assertThat(item.getPlanoContas().getId()).isEqualTo(5L);
        assertThat(item.getValorPrevisto()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(item.getValorRealizado()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(em).getReference(PlanoContas.class, 5L);
    }

    @Test
    void toItemEntity_whenPlanoContasIdIsNull_planoContasIsNull() {
        var orcamento = OrcamentoAnual.builder().id(1L).exercicio(2026)
                .status(StatusOrcamento.RASCUNHO).itens(List.of()).build();
        var dto = new CreateItemOrcamentoDTO(null, new BigDecimal("100.00"));

        var item = mapper.toItemEntity(orcamento, dto);

        assertThat(item.getPlanoContas()).isNull();
    }
}
