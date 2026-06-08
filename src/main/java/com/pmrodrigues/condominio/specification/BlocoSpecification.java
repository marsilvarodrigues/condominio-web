package com.pmrodrigues.condominio.specification;

import com.pmrodrigues.condominio.model.Bloco;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Bloco} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlocoSpecification {

  /**
   * Returns a specification that matches blocos whose name contains the given substring
   * (case-insensitive), or {@code null} when the filter is absent.
   *
   * @param bloco the substring to search for, or {@code null}/blank to skip this predicate
   * @return a case-insensitive LIKE predicate on bloco name, or {@code null}
   */
  public static Specification<Bloco> hasBloco(String bloco) {
    if (bloco == null || bloco.isBlank()) {
      return null;
    }
    return (root, query, cb) ->
        cb.like(cb.lower(root.get("bloco")), "%" + bloco.toLowerCase() + "%");
  }
}
