package com.pmrodrigues.morador.specification;

import com.pmrodrigues.morador.model.Morador;
import com.pmrodrigues.morador.model.Pessoa;
import com.pmrodrigues.morador.model.ProprietarioPessoaFisica;
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
   * <p>Uses the explicit {@code pessoaTipo} field instead of {@code root.type()} to avoid
   * database-specific CASE expressions that are incompatible with H2 in JOINED inheritance.
   *
   * @param tipo discriminator value, or {@code null} to skip
   * @return predicate on pessoa_tipo, or {@code null}
   */
  public static Specification<Pessoa> hasTipo(String tipo) {
    if (tipo == null || tipo.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("pessoaTipo"), tipo);
  }

  /**
   * Returns an exact-match specification on the cpf field, matching against both {@link Morador}
   * and {@link ProprietarioPessoaFisica} subtypes, or {@code null} when absent.
   *
   * <p>Uses {@code cb.treat()} to downcast the root to each concrete subtype, avoiding the
   * ambiguous-attribute error that occurs when accessing {@code cpf} directly on {@link Pessoa}
   * (since multiple subtypes declare the same attribute name in separate JOINED tables).
   *
   * @param cpf the CPF to match, or {@code null}/blank to skip
   * @return predicate on cpf across relevant subtypes, or {@code null}
   */
  public static Specification<Pessoa> hasCpf(String cpf) {
    if (cpf == null || cpf.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.or(
        cb.equal(cb.treat(root, Morador.class).get("cpf"), cpf),
        cb.equal(cb.treat(root, ProprietarioPessoaFisica.class).get("cpf"), cpf)
    );
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
