package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.LancamentoBancario;
import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link LancamentoBancario} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LancamentoBancarioSpecification {

    /**
     * Returns a specification restricting results to a specific conta bancaria.
     */
    public static Specification<LancamentoBancario> hasContaBancaria(Long contaBancariaId) {
        if (contaBancariaId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("contaBancaria").get("id"), contaBancariaId);
    }

    /**
     * Returns a specification matching lancamentos of the given tipo.
     */
    public static Specification<LancamentoBancario> hasTipo(TipoLancamento tipo) {
        if (tipo == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    /**
     * Returns a specification matching lancamentos with the given origem.
     */
    public static Specification<LancamentoBancario> hasOrigem(OrigemLancamento origem) {
        if (origem == null) return null;
        return (root, query, cb) -> cb.equal(root.get("origem"), origem);
    }

    /**
     * Returns a specification matching lancamentos with the given status.
     */
    public static Specification<LancamentoBancario> hasStatus(StatusLancamento status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Returns a specification matching lancamentos on or after the given date.
     */
    public static Specification<LancamentoBancario> dataInicio(LocalDate dataInicio) {
        if (dataInicio == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataLancamento"), dataInicio);
    }

    /**
     * Returns a specification matching lancamentos on or before the given date.
     */
    public static Specification<LancamentoBancario> dataFim(LocalDate dataFim) {
        if (dataFim == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataLancamento"), dataFim);
    }
}
