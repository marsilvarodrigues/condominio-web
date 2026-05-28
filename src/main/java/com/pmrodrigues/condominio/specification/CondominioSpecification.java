package com.pmrodrigues.condominio.specification;

import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.model.Condominio;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Condominio} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CondominioSpecification {

    /**
     * Returns a specification that matches condominios whose nome contains the given substring (case-insensitive), or {@code null} when the filter is absent.
     *
     * @param nome the substring to search for, or {@code null}/blank to skip this predicate
     * @return a case-insensitive LIKE predicate on nome, or {@code null}
     */
    public static Specification<Condominio> hasNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    /**
     * Returns a specification that matches a condominio with the exact CNPJ, or {@code null} when the filter is absent.
     *
     * @param cnpj the exact CNPJ to match, or {@code null}/blank to skip this predicate
     * @return an equality predicate on cnpj, or {@code null}
     */
    public static Specification<Condominio> hasCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("cnpj"), cnpj);
    }
}
