package com.pmrodrigues.financeiro.util;

import com.pmrodrigues.financeiro.model.ItemExtrato;
import com.pmrodrigues.financeiro.model.ItemOrcamento;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-function utility that scores how well an {@link ItemOrcamento} matches a given {@link
 * ItemExtrato}. Score is 0–100 (higher is better).
 *
 * <p>Scoring breakdown:
 *
 * <ul>
 *   <li>0–50 points — keyword overlap between statement description and account name
 *   <li>0–30 points — CREDITO↔RECEITA / DEBITO↔DESPESA type compatibility
 *   <li>0–20 points — value proximity (full 20 pts when values match; 0 when ≥100 % apart)
 * </ul>
 */
public final class SugestaoScoreCalculator {

  private SugestaoScoreCalculator() {}

  /**
   * Returns a composite score (0–100) measuring how well {@code orcamento} fits {@code extrato}.
   */
  public static int calcularScore(ItemExtrato extrato, ItemOrcamento orcamento) {
    int score =
        keywordScore(extrato.getDescricao(), orcamento.getPlanoContas().getDescricao())
            + tipoScore(extrato.getTipo(), orcamento.getPlanoContas().getTipo())
            + valorScore(extrato.getValor(), orcamento.getValorPrevisto());
    return Math.min(score, 100);
  }

  /** Returns {@code true} when the lancamento type is compatible with the account type. */
  public static boolean tipoCompativel(TipoLancamento tipo, TipoConta tipoConta) {
    if (tipo == null || tipoConta == null) {
      return false;
    }
    return (tipo == TipoLancamento.CREDITO && tipoConta == TipoConta.RECEITA)
        || (tipo == TipoLancamento.DEBITO && tipoConta == TipoConta.DESPESA);
  }

  /**
   * Returns a human-readable explanation of why {@code orcamento} was suggested for {@code
   * extrato}.
   */
  public static String motivoSugestao(ItemExtrato extrato, ItemOrcamento orcamento) {
    List<String> motivos = new ArrayList<>();
    if (tipoScore(extrato.getTipo(), orcamento.getPlanoContas().getTipo()) > 0) {
      motivos.add("tipo compatível");
    }
    if (keywordScore(extrato.getDescricao(), orcamento.getPlanoContas().getDescricao()) > 10) {
      motivos.add("palavras-chave em comum");
    }
    if (valorScore(extrato.getValor(), orcamento.getValorPrevisto()) > 10) {
      motivos.add("valor próximo ao previsto");
    }
    return motivos.isEmpty() ? "melhor correspondência disponível" : String.join(", ", motivos);
  }

  /**
   * Returns 0–50 points based on keyword overlap between the statement description and the account
   * name.
   *
   * @param descricaoExtrato the statement line description, may be null
   * @param descricaoOrcamento the budget account description, may be null
   * @return keyword overlap score in the range [0, 50]
   */
  public static int keywordScore(String descricaoExtrato, String descricaoOrcamento) {
    if (descricaoExtrato == null || descricaoOrcamento == null) {
      return 0;
    }
    String[] tokens = descricaoOrcamento.toLowerCase().split("\\s+");
    if (tokens.length == 0) {
      return 0;
    }
    String texto = descricaoExtrato.toLowerCase();
    long matches = Arrays.stream(tokens).filter(texto::contains).count();
    return (int) Math.min(50, (matches * 50) / tokens.length);
  }

  /**
   * Returns 30 when the lancamento type is compatible with the account type, or 0 otherwise.
   *
   * @param tipo the lancamento type from the bank statement
   * @param tipoConta the account type from the budget item
   * @return 30 if compatible, 0 otherwise
   */
  public static int tipoScore(TipoLancamento tipo, TipoConta tipoConta) {
    return tipoCompativel(tipo, tipoConta) ? 30 : 0;
  }

  /**
   * Returns 0–20 points based on value proximity; full 20 pts when values match exactly, 0 when
   * they differ by 100 % or more.
   *
   * @param valorExtrato the transaction amount from the bank statement
   * @param valorPrevisto the predicted amount from the budget item
   * @return value proximity score in the range [0, 20]
   */
  public static int valorScore(BigDecimal valorExtrato, BigDecimal valorPrevisto) {
    if (valorExtrato == null
        || valorPrevisto == null
        || valorPrevisto.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }
    BigDecimal diff = valorExtrato.subtract(valorPrevisto).abs();
    BigDecimal ratio = diff.divide(valorPrevisto, 4, RoundingMode.HALF_UP);
    if (ratio.compareTo(BigDecimal.ONE) >= 0) {
      return 0;
    }
    return (int) Math.round(20 * (1.0 - ratio.doubleValue()));
  }
}
