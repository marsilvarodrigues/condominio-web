package com.pmrodrigues.cobranca.specification;

import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory for JPA {@link Specification} predicates used to filter {@link Cobranca} queries.
 *
 * <p>Each method returns {@code null} when the filter value is absent — {@code null} predicates are
 * safely ignored by {@link Specification#allOf(Specification[])}.
 */
public final class CobrancaSpecification {

  private CobrancaSpecification() {}

  /**
   * Restricts results to the given apartment.
   *
   * @param apartamentoId apartment primary key; returns null predicate when absent
   */
  public static Specification<Cobranca> hasApartamento(Long apartamentoId) {
    return (root, query, cb) ->
        apartamentoId == null ? null : cb.equal(root.get("apartamento").get("id"), apartamentoId);
  }

  /**
   * Restricts results to charges with the given lifecycle status.
   *
   * @param status lifecycle status; returns null predicate when absent
   */
  public static Specification<Cobranca> hasStatus(StatusCobranca status) {
    return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
  }

  /**
   * Restricts results to charges whose due date is on or after the given date.
   *
   * @param from lower bound (inclusive); returns null predicate when absent
   */
  public static Specification<Cobranca> vencimentoFrom(LocalDate from) {
    return (root, query, cb) ->
        from == null ? null : cb.greaterThanOrEqualTo(root.get("vencimento"), from);
  }

  /**
   * Restricts results to charges whose due date is on or before the given date.
   *
   * @param to upper bound (inclusive); returns null predicate when absent
   */
  public static Specification<Cobranca> vencimentoTo(LocalDate to) {
    return (root, query, cb) ->
        to == null ? null : cb.lessThanOrEqualTo(root.get("vencimento"), to);
  }

  /**
   * Restricts results by whether the billing email was sent.
   *
   * @param emailEnviado filter value; returns null predicate when absent
   */
  public static Specification<Cobranca> emailEnviado(Boolean emailEnviado) {
    return (root, query, cb) ->
        emailEnviado == null ? null : cb.equal(root.get("emailEnviado"), emailEnviado);
  }
}
