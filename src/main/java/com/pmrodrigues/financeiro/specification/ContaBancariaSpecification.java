package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link ContaBancaria} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ContaBancariaSpecification {

    /**
     * Returns a specification matching accounts of the given type.
     */
    public static Specification<ContaBancaria> hasTipo(TipoContaBancaria tipo) {
        if (tipo == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    /**
     * Returns a specification matching accounts with the given ativa status.
     */
    public static Specification<ContaBancaria> isAtiva(Boolean ativa) {
        if (ativa == null) return null;
        return (root, query, cb) -> cb.equal(root.get("ativa"), ativa);
    }

    /**
     * Returns a specification matching accounts whose agencia starts with the given prefix.
     */
    public static Specification<ContaBancaria> hasAgencia(String agencia) {
        if (agencia == null || agencia.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(root.get("agencia"), agencia + "%");
    }

    /**
     * Returns a specification matching accounts whose conta starts with the given prefix.
     */
    public static Specification<ContaBancaria> hasConta(String conta) {
        if (conta == null || conta.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(root.get("conta"), conta + "%");
    }
}
