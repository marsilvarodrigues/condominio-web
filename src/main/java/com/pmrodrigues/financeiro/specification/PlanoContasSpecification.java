package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.PlanoContas;
import com.pmrodrigues.financeiro.model.TipoConta;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link PlanoContas}
 * entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlanoContasSpecification {

  /**
   * Returns a specification filtering by account type, or {@code null} when type is absent.
   *
   * @param tipo the account type to filter by, or {@code null} to skip this predicate
   * @return an equality predicate on tipo, or {@code null}
   */
  public static Specification<PlanoContas> hasTipo(TipoConta tipo) {
    if (tipo == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
  }

  /**
   * Returns a specification filtering by parent node id, or {@code null} when paiId is absent.
   *
   * @param paiId the parent node id to filter by, or {@code null} to skip this predicate
   * @return an equality predicate on pai.id, or {@code null}
   */
  public static Specification<PlanoContas> hasPai(Long paiId) {
    if (paiId == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("pai").get("id"), paiId);
  }
}
