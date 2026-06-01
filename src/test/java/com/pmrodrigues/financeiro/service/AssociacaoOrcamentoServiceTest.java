package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.dto.ContribuicaoResponse;
import com.pmrodrigues.financeiro.dto.DesassociarItemOrcamentoRequest;
import com.pmrodrigues.financeiro.dto.ItemExtratoComOrcamentoResponse;
import com.pmrodrigues.financeiro.dto.ItemOrcamentoResumoResponse;
import com.pmrodrigues.financeiro.mapper.ItemExtratoMapper;
import com.pmrodrigues.financeiro.model.*;
import com.pmrodrigues.financeiro.repository.ItemExtratoRepository;
import com.pmrodrigues.financeiro.repository.ItemOrcamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssociacaoOrcamentoServiceTest {

    @Mock ItemExtratoRepository itemExtratoRepository;
    @Mock ItemOrcamentoRepository itemOrcamentoRepository;
    @Mock ItemExtratoMapper mapper;

    AssociacaoOrcamentoService service;

    @BeforeEach
    void setUp() {
        service = new AssociacaoOrcamentoService(itemExtratoRepository, itemOrcamentoRepository, mapper);
        lenient().when(mapper.toDTO(any(ItemExtrato.class))).thenAnswer(inv -> {
            ItemExtrato e = inv.getArgument(0);
            ItemOrcamentoResumoResponse orcamento = null;
            if (e.getItemOrcamento() != null) {
                orcamento = new ItemOrcamentoResumoResponse(
                        e.getItemOrcamento().getId(), "1.1", "Test",
                        BigDecimal.valueOf(500), BigDecimal.ZERO);
            }
            return new ItemExtratoComOrcamentoResponse(
                    e.getId(), e.getDataLancamento(), e.getValor(),
                    e.getTipo(), e.getDescricao(), e.getStatus(), orcamento);
        });
    }

    private PlanoContas planoContas(Long id, String codigo, String descricao, TipoConta tipo) {
        return PlanoContas.builder().id(id).codigo(codigo).descricao(descricao).tipo(tipo).build();
    }

    private ItemOrcamento itemOrcamento(Long id, PlanoContas pc, BigDecimal valorPrevisto) {
        return ItemOrcamento.builder().id(id).planoContas(pc)
                .valorPrevisto(valorPrevisto).valorRealizado(BigDecimal.ZERO).build();
    }

    private ItemExtrato itemExtrato(Long id, TipoLancamento tipo, BigDecimal valor, String descricao) {
        return ItemExtrato.builder().id(id).dataLancamento(LocalDate.now())
                .valor(valor).tipo(tipo).descricao(descricao).status(StatusItemExtrato.PENDENTE).build();
    }

    // ── associar ──────────────────────────────────────────────────────────

    @Test
    void associar_happyPath_setsItemOrcamentoAndReturnsDTO() {
        var pc = planoContas(1L, "1.1", "Manutenção", TipoConta.DESPESA);
        var orcamento = itemOrcamento(10L, pc, BigDecimal.valueOf(500));
        var extrato = itemExtrato(20L, TipoLancamento.DEBITO, BigDecimal.valueOf(450), "Manutenção predial");

        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));
        when(itemOrcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento));
        when(itemExtratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.associar(20L, 10L);

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.tipo()).isEqualTo(TipoLancamento.DEBITO);
        assertThat(result.itemOrcamento()).isNotNull();
        assertThat(result.itemOrcamento().id()).isEqualTo(10L);
        verify(itemExtratoRepository).save(extrato);
        verify(mapper).toDTO(extrato);
    }

    @Test
    void associar_whenExtratoNotFound_throws404() {
        when(itemExtratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.associar(99L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(itemExtratoRepository, never()).save(any());
        verify(mapper, never()).toDTO(any());
    }

    @Test
    void associar_whenOrcamentoNotFound_throws404() {
        var extrato = itemExtrato(20L, TipoLancamento.DEBITO, BigDecimal.TEN, "Teste");
        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));
        when(itemOrcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.associar(20L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(itemExtratoRepository, never()).save(any());
    }

    // ── desassociar ───────────────────────────────────────────────────────

    @Test
    void desassociar_happyPath_clearsItemOrcamentoAndReturnsDTO() {
        var pc = planoContas(1L, "2.1", "Receita taxa", TipoConta.RECEITA);
        var orcamento = itemOrcamento(10L, pc, BigDecimal.valueOf(300));
        var extrato = itemExtrato(20L, TipoLancamento.CREDITO, BigDecimal.valueOf(300), "Taxa cond.");
        extrato.setItemOrcamento(orcamento);

        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));
        when(itemExtratoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.desassociar(20L, new DesassociarItemOrcamentoRequest("Associação incorreta"));

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.itemOrcamento()).isNull();
        assertThat(extrato.getItemOrcamento()).isNull();
        verify(itemExtratoRepository).save(extrato);
        verify(mapper).toDTO(extrato);
    }

    @Test
    void desassociar_whenExtratoNotFound_throws404() {
        when(itemExtratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desassociar(99L, new DesassociarItemOrcamentoRequest("justificativa")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(mapper, never()).toDTO(any());
    }

    // ── sugerirItemOrcamento ──────────────────────────────────────────────

    @Test
    void sugerir_returnsMatchesSortedByScoreDesc() {
        var extrato = itemExtrato(20L, TipoLancamento.CREDITO, BigDecimal.valueOf(500), "Taxa condominial");
        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));

        var pcReceita = planoContas(1L, "1.1", "Taxa condominial", TipoConta.RECEITA);
        var pcDespesa = planoContas(2L, "2.1", "Manutenção", TipoConta.DESPESA);
        when(itemOrcamentoRepository.findAll()).thenReturn(List.of(
                itemOrcamento(10L, pcReceita, BigDecimal.valueOf(500)),
                itemOrcamento(11L, pcDespesa, BigDecimal.valueOf(200))
        ));

        var result = service.sugerirItemOrcamento(20L);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).tipoCompativel()).isTrue();
        assertThat(result.get(0).score()).isGreaterThanOrEqualTo(result.get(result.size() - 1).score());
    }

    @Test
    void sugerir_excludesZeroScoreCandidates() {
        var extrato = itemExtrato(20L, TipoLancamento.CREDITO, BigDecimal.valueOf(500), "Taxa");
        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));

        var pc = planoContas(1L, "2.1", "Folha pagamento", TipoConta.DESPESA);
        when(itemOrcamentoRepository.findAll()).thenReturn(List.of(
                itemOrcamento(10L, pc, BigDecimal.valueOf(50000))
        ));

        assertThat(service.sugerirItemOrcamento(20L)).isEmpty();
    }

    @Test
    void sugerir_limitsToFiveResults() {
        var extrato = itemExtrato(20L, TipoLancamento.CREDITO, BigDecimal.valueOf(100), "Taxa");
        when(itemExtratoRepository.findById(20L)).thenReturn(Optional.of(extrato));

        var pc = planoContas(1L, "1.1", "Taxa", TipoConta.RECEITA);
        when(itemOrcamentoRepository.findAll()).thenReturn(List.of(
                itemOrcamento(1L, pc, BigDecimal.valueOf(100)),
                itemOrcamento(2L, pc, BigDecimal.valueOf(100)),
                itemOrcamento(3L, pc, BigDecimal.valueOf(100)),
                itemOrcamento(4L, pc, BigDecimal.valueOf(100)),
                itemOrcamento(5L, pc, BigDecimal.valueOf(100)),
                itemOrcamento(6L, pc, BigDecimal.valueOf(100))
        ));

        assertThat(service.sugerirItemOrcamento(20L)).hasSize(5);
    }

    @Test
    void sugerir_whenExtratoNotFound_throws404() {
        when(itemExtratoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sugerirItemOrcamento(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ── calcularContribuicao ──────────────────────────────────────────────

    @Test
    void calcularContribuicao_sumsValoresAndComputesPercentage() {
        var pc = planoContas(1L, "2.1", "Gastos gerais", TipoConta.DESPESA);
        var orcamento = itemOrcamento(10L, pc, BigDecimal.valueOf(1000));
        when(itemOrcamentoRepository.findById(10L)).thenReturn(Optional.of(orcamento));
        when(itemExtratoRepository.findByItemOrcamentoId(10L)).thenReturn(List.of(
                itemExtrato(1L, TipoLancamento.DEBITO, BigDecimal.valueOf(300), "Gás"),
                itemExtrato(2L, TipoLancamento.DEBITO, BigDecimal.valueOf(200), "Água")
        ));

        var result = service.calcularContribuicao(10L);

        assertThat(result.itemOrcamentoId()).isEqualTo(10L);
        assertThat(result.valorPrevisto()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(result.valorRealizado()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(result.percentualRealizado()).isEqualByComparingTo(new java.math.BigDecimal("50.00"));
        assertThat(result.totalItensAssociados()).isEqualTo(2);
        verify(itemOrcamentoRepository).findById(10L);
        verify(itemExtratoRepository).findByItemOrcamentoId(10L);
    }

    @Test
    void calcularContribuicao_whenNoItensAssociados_returnsZero() {
        var pc = planoContas(1L, "1.1", "Receita", TipoConta.RECEITA);
        when(itemOrcamentoRepository.findById(10L)).thenReturn(Optional.of(itemOrcamento(10L, pc, BigDecimal.valueOf(500))));
        when(itemExtratoRepository.findByItemOrcamentoId(10L)).thenReturn(List.of());

        var result = service.calcularContribuicao(10L);

        assertThat(result.valorRealizado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.percentualRealizado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalItensAssociados()).isZero();
    }

    @Test
    void calcularContribuicao_whenOrcamentoNotFound_throws404() {
        when(itemOrcamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calcularContribuicao(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(itemExtratoRepository, never()).findByItemOrcamentoId(any());
    }
}
