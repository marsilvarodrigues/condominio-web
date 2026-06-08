package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.Despesa;
import com.pmrodrigues.financeiro.model.StatusRateio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link Despesa} entities. */
@Repository
public interface DespesaRepository
    extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {

  /**
   * Returns all non-deleted despesas with the given rateio status for the current tenant.
   *
   * @param status the rateio status to filter by
   */
  List<Despesa> findByRateioStatus(StatusRateio status);

  /** Returns all non-deleted despesas with PENDENTE or ERRO status for the current tenant. */
  @Query("SELECT d FROM Despesa d WHERE d.rateioStatus IN ('PENDENTE', 'ERRO')")
  List<Despesa> findPendentesOuErro();

  /** Marks all non-deleted despesas of the given condominium as PENDENTE. */
  @Modifying
  @Query(
      "UPDATE Despesa d SET d.rateioStatus = 'PENDENTE' WHERE d.grupoDespesa.id IN "
          + "(SELECT g.id FROM GrupoDespesa g WHERE g.condominio.id = :condominioId)")
  void marcarTodosPendente(@Param("condominioId") Long condominioId);
}
