package com.pmrodrigues.morador.specification;

import com.pmrodrigues.morador.model.Proprietario;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link Proprietario} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProprietarioSpecification {

    /**
     * Returns a specification matching proprietarios that own the given apartment, or {@code null} when absent.
     *
     * @param apartamentoId apartment FK, or {@code null} to skip
     * @return predicate on the apartamentos collection, or {@code null}
     */
    public static Specification<Proprietario> hasApartamentoId(Long apartamentoId) {
        if (apartamentoId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("apartamentos").get("id"), apartamentoId);
    }
}
