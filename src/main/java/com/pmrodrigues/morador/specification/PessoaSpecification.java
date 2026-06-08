package com.pmrodrigues.morador.specification;

import com.pmrodrigues.morador.model.Pessoa;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Pessoa}
 * entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PessoaSpecification {

  /**
   * Returns a case-insensitive LIKE specification on the nome field, or {@code null} when absent.
   *
   * @param nome substring to search for, or {@code null}/blank to skip
   * @return predicate on nome, or {@code null}
   */
  public static Specification<Pessoa> hasNome(String nome) {
    if (nome == null || nome.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + nome.toLowerCase() + "%");
  }

  /**
   * Returns an exact-match specification on the {@code pessoa_tipo} discriminator column, or {@code
   * null} when absent. Valid values: MORADOR, PROP_PF, PROP_PJ.
   *
   * @param tipo discriminator value, or {@code null} to skip
   * @return predicate on pessoa_tipo, or {@code null}
   */
  public static Specification<Pessoa> hasTipo(String tipo) {
    if (tipo == null || tipo.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.type().as(String.class), tipo);
  }

  /**
   * Returns an exact-match specification on the cpf field, or {@code null} when absent.
   *
   * @param cpf the CPF to match, or {@code null}/blank to skip
   * @return predicate on cpf, or {@code null}
   */
  public static Specification<Pessoa> hasCpf(String cpf) {
    if (cpf == null || cpf.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("cpf"), cpf);
  }

  /**
   * Returns an exact-match specification on the email field from the parent {@code users} table, or
   * {@code null} when absent.
   *
   * @param email the email to match, or {@code null}/blank to skip
   * @return predicate on email, or {@code null}
   */
  public static Specification<Pessoa> hasEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("email"), email);
  }
}
