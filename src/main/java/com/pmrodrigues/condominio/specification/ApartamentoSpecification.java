package com.pmrodrigues.condominio.specification;

import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Apartamento} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApartamentoSpecification {

    /**
     * Returns a specification that matches apartamentos belonging to the given bloco, or {@code null} when the filter is absent.
     *
     * @param blocoId the bloco FK to filter by, or {@code null} to skip this predicate
     * @return a predicate on bloco id, or {@code null}
     */
    public static Specification<Apartamento> hasBlocoId(Long blocoId) {
        if (blocoId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("bloco").get("id"), blocoId);
    }

    /**
     * Returns a specification that matches apartamentos whose numero contains the given substring (case-insensitive), or {@code null} when the filter is absent.
     *
     * @param numero the substring to search for, or {@code null}/blank to skip this predicate
     * @return a case-insensitive LIKE predicate on numero, or {@code null}
     */
    public static Specification<Apartamento> hasNumero(String numero) {
        if (numero == null || numero.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%");
    }
}
