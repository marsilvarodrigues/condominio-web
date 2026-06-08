package com.pmrodrigues.commons.specification;

import com.pmrodrigues.commons.model.Banco;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Banco} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BancoSpecification {

  /**
   * Returns a specification matching bancos whose code starts with the given prefix
   * (case-insensitive).
   */
  public static Specification<Banco> hasCodigo(String codigo) {
    if (codigo == null || codigo.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.like(cb.lower(root.get("codigo")), codigo.toLowerCase() + "%");
  }

  /**
   * Returns a specification matching bancos whose name contains the given substring
   * (case-insensitive).
   */
  public static Specification<Banco> hasNome(String nome) {
    if (nome == null || nome.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
  }
}
