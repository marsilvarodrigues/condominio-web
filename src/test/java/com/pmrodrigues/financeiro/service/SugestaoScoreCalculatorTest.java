package com.pmrodrigues.financeiro.service;

import com.pmrodrigues.financeiro.model.*;
import com.pmrodrigues.financeiro.util.SugestaoScoreCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SugestaoScoreCalculatorTest {

    private PlanoContas planoContas(String codigo, String descricao, TipoConta tipo) {
        return PlanoContas.builder().id(1L).codigo(codigo).descricao(descricao).tipo(tipo).build();
    }

    private ItemOrcamento orcamento(PlanoContas pc, BigDecimal valorPrevisto) {
        return ItemOrcamento.builder().id(1L).planoContas(pc).valorPrevisto(valorPrevisto)
                .valorRealizado(BigDecimal.ZERO).build();
    }

    private ItemExtrato extrato(TipoLancamento tipo, BigDecimal valor, String descricao) {
        return ItemExtrato.builder().id(1L).dataLancamento(LocalDate.now())
                .tipo(tipo).valor(valor).descricao(descricao).status(StatusItemExtrato.PENDENTE).build();
    }

    // ── keywordScore ──────────────────────────────────────────────────────

    @Test
    void keywordScore_exactMatch_returns50() {
        int score = SugestaoScoreCalculator.keywordScore("taxa condominial", "taxa condominial");
        assertThat(score).isEqualTo(50);
    }

    @Test
    void keywordScore_noMatch_returns0() {
        int score = SugestaoScoreCalculator.keywordScore("gás", "taxa condominial");
        assertThat(score).isEqualTo(0);
    }

    @Test
    void keywordScore_nullDescricao_returns0() {
        assertThat(SugestaoScoreCalculator.keywordScore(null, "taxa")).isEqualTo(0);
        assertThat(SugestaoScoreCalculator.keywordScore("taxa", null)).isEqualTo(0);
    }

    // ── tipoScore ─────────────────────────────────────────────────────────

    @Test
    void tipoScore_creditoReceita_returns30() {
        assertThat(SugestaoScoreCalculator.tipoScore(TipoLancamento.CREDITO, TipoConta.RECEITA)).isEqualTo(30);
    }

    @Test
    void tipoScore_debitoDespesa_returns30() {
        assertThat(SugestaoScoreCalculator.tipoScore(TipoLancamento.DEBITO, TipoConta.DESPESA)).isEqualTo(30);
    }

    @Test
    void tipoScore_creditoDespesa_returns0() {
        assertThat(SugestaoScoreCalculator.tipoScore(TipoLancamento.CREDITO, TipoConta.DESPESA)).isEqualTo(0);
    }

    @Test
    void tipoScore_nullArgs_returns0() {
        assertThat(SugestaoScoreCalculator.tipoScore(null, TipoConta.RECEITA)).isEqualTo(0);
        assertThat(SugestaoScoreCalculator.tipoScore(TipoLancamento.CREDITO, null)).isEqualTo(0);
    }

    // ── valorScore ────────────────────────────────────────────────────────

    @Test
    void valorScore_exactMatch_returns20() {
        int score = SugestaoScoreCalculator.valorScore(BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        assertThat(score).isEqualTo(20);
    }

    @Test
    void valorScore_100percentDifference_returns0() {
        // diff = 200, previsto = 100 → ratio = 2.0 ≥ 1.0 → 0
        int score = SugestaoScoreCalculator.valorScore(BigDecimal.valueOf(300), BigDecimal.valueOf(100));
        assertThat(score).isZero();
    }

    @Test
    void valorScore_zeroPrevisto_returns0() {
        assertThat(SugestaoScoreCalculator.valorScore(BigDecimal.TEN, BigDecimal.ZERO)).isEqualTo(0);
    }

    @Test
    void valorScore_nullArgs_returns0() {
        assertThat(SugestaoScoreCalculator.valorScore(null, BigDecimal.TEN)).isEqualTo(0);
        assertThat(SugestaoScoreCalculator.valorScore(BigDecimal.TEN, null)).isEqualTo(0);
    }

    // ── tipoCompativel ────────────────────────────────────────────────────

    @Test
    void tipoCompativel_creditoReceita_returnsTrue() {
        assertThat(SugestaoScoreCalculator.tipoCompativel(TipoLancamento.CREDITO, TipoConta.RECEITA)).isTrue();
    }

    @Test
    void tipoCompativel_debitoDespesa_returnsTrue() {
        assertThat(SugestaoScoreCalculator.tipoCompativel(TipoLancamento.DEBITO, TipoConta.DESPESA)).isTrue();
    }

    @Test
    void tipoCompativel_mismatch_returnsFalse() {
        assertThat(SugestaoScoreCalculator.tipoCompativel(TipoLancamento.CREDITO, TipoConta.DESPESA)).isFalse();
    }

    // ── calcularScore ─────────────────────────────────────────────────────

    @Test
    void calcularScore_perfectMatch_returnsHighScore() {
        var e = extrato(TipoLancamento.CREDITO, BigDecimal.valueOf(500), "Taxa condominial");
        var pc = planoContas("1.1", "Taxa condominial", TipoConta.RECEITA);
        var o = orcamento(pc, BigDecimal.valueOf(500));

        int score = SugestaoScoreCalculator.calcularScore(e, o);
        assertThat(score).isGreaterThanOrEqualTo(80);
    }

    @Test
    void calcularScore_noMatch_returnsZero() {
        var e = extrato(TipoLancamento.CREDITO, BigDecimal.valueOf(999), "Pagamento GLP");
        var pc = planoContas("2.1", "Manutenção estrutura", TipoConta.DESPESA);
        var o = orcamento(pc, BigDecimal.valueOf(50000));

        int score = SugestaoScoreCalculator.calcularScore(e, o);
        assertThat(score).isZero();
    }

    @Test
    void calcularScore_cappedAt100() {
        var e = extrato(TipoLancamento.CREDITO, BigDecimal.valueOf(100), "taxa taxa taxa taxa taxa");
        var pc = planoContas("1.1", "taxa taxa taxa taxa taxa", TipoConta.RECEITA);
        var o = orcamento(pc, BigDecimal.valueOf(100));

        int score = SugestaoScoreCalculator.calcularScore(e, o);
        assertThat(score).isLessThanOrEqualTo(100);
    }

    // ── motivoSugestao ────────────────────────────────────────────────────

    @Test
    void motivoSugestao_noSignals_returnsFallback() {
        var e = extrato(TipoLancamento.CREDITO, BigDecimal.valueOf(999), "XYZ");
        var pc = planoContas("2.1", "ABCDEF", TipoConta.DESPESA);
        var o = orcamento(pc, BigDecimal.valueOf(50000));

        String motivo = SugestaoScoreCalculator.motivoSugestao(e, o);
        assertThat(motivo).isEqualTo("melhor correspondência disponível");
    }

    @Test
    void motivoSugestao_withKeywords_includesKeywords() {
        var e = extrato(TipoLancamento.DEBITO, BigDecimal.valueOf(100), "manutenção predial");
        var pc = planoContas("2.1", "manutenção", TipoConta.DESPESA);
        var o = orcamento(pc, BigDecimal.valueOf(100));

        String motivo = SugestaoScoreCalculator.motivoSugestao(e, o);
        assertThat(motivo).contains("palavras-chave em comum");
    }
}
