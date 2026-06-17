package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.CotaRateio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link CotaRateio} records. */
@Repository
public interface CotaRateioRepository extends JpaRepository<CotaRateio, Long> {

  /**
   * Removes all quota records for the given expense. Called before a recalculation so that old
   * quotas are replaced cleanly.
   */
  @Modifying
  @Query("DELETE FROM CotaRateio c WHERE c.despesa.id = :despesaId")
  void deleteByDespesaId(@Param("despesaId") Long despesaId);

  /**
   * Returns all quota records belonging to the given rateio execution, with {@code apartamento} and
   * its {@code bloco} eagerly fetched to avoid N+1 queries when the billing service accesses those
   * fields per cota.
   *
   * @param rateioExecucaoId primary key of the {@code RateioExecucao}
   * @return list of cotas (may be empty if the execution had no units)
   */
  @Query(
      "SELECT c FROM CotaRateio c JOIN FETCH c.apartamento a JOIN FETCH a.bloco "
          + "WHERE c.rateioExecucao.id = :rateioExecucaoId")
  List<CotaRateio> findByRateioExecucaoId(@Param("rateioExecucaoId") Long rateioExecucaoId);
}
