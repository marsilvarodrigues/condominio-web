package com.pmrodrigues.commons.specification;

import com.pmrodrigues.commons.model.Estado;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Estado} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EstadoSpecification {

    /**
     * Returns a specification that matches estados whose name contains the given substring (case-insensitive), or {@code null} when the filter is absent.
     *
     * @param nome the substring to search for, or {@code null}/blank to skip this predicate
     * @return a case-insensitive LIKE predicate on nome, or {@code null}
     */
    public static Specification<Estado> hasNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    /**
     * Returns a specification that matches estados with the given UF abbreviation (case-insensitive exact match), or {@code null} when the filter is absent.
     *
     * @param uf the two-letter state abbreviation, or {@code null}/blank to skip this predicate
     * @return a case-insensitive equality predicate on uf, or {@code null}
     */
    public static Specification<Estado> hasUf(String uf) {
        if (uf == null || uf.isBlank()) return null;
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("uf")), uf.toLowerCase());
    }
}
