package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.GrupoDespesa;
import com.pmrodrigues.financeiro.model.TipoRateio;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link GrupoDespesa} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GrupoDespesaSpecification {

    /**
     * Filters by rateio type, or no-ops when {@code tipoRateio} is null.
     */
    public static Specification<GrupoDespesa> hasTipoRateio(TipoRateio tipoRateio) {
        if (tipoRateio == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tipoRateio"), tipoRateio);
    }

    /**
     * Filters by escopo, or no-ops when {@code escopo} is null.
     */
    public static Specification<GrupoDespesa> hasEscopo(EscopoRateio escopo) {
        if (escopo == null) return null;
        return (root, query, cb) -> cb.equal(root.get("escopo"), escopo);
    }
}
