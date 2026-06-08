package com.pmrodrigues.financeiro.specification;

import com.pmrodrigues.financeiro.model.RateioExecucao;
import com.pmrodrigues.financeiro.model.StatusExecucaoRateio;
import com.pmrodrigues.financeiro.model.TipoExecucaoRateio;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link RateioExecucao}
 * records.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RateioExecucaoSpecification {

  /** Filters by execution status, or no-ops when {@code status} is null. */
  public static Specification<RateioExecucao> hasStatus(StatusExecucaoRateio status) {
    if (status == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  /** Filters by execution type, or no-ops when {@code tipoExecucao} is null. */
  public static Specification<RateioExecucao> hasTipoExecucao(TipoExecucaoRateio tipoExecucao) {
    if (tipoExecucao == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("tipoExecucao"), tipoExecucao);
  }

  /** Filters executions on or after the given date-time, or no-ops when null. */
  public static Specification<RateioExecucao> dataInicio(LocalDateTime inicio) {
    if (inicio == null) {
      return null;
    }
    return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dataExecucao"), inicio);
  }

  /** Filters executions on or before the given date-time, or no-ops when null. */
  public static Specification<RateioExecucao> dataFim(LocalDateTime fim) {
    if (fim == null) {
      return null;
    }
    return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dataExecucao"), fim);
  }
}
