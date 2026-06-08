package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link OrcamentoAnual}
 * entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrcamentoAnualSpecification {

  /** Returns a specification filtering by exercise year, or {@code null} when absent. */
  public static Specification<OrcamentoAnual> hasExercicio(Integer exercicio) {
    if (exercicio == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("exercicio"), exercicio);
  }

  /** Returns a specification filtering by status, or {@code null} when absent. */
  public static Specification<OrcamentoAnual> hasStatus(StatusOrcamento status) {
    if (status == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }
}
