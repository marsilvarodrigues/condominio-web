package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.CotaRateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link CotaRateio} records.
 */
@Repository
public interface CotaRateioRepository extends JpaRepository<CotaRateio, Long> {

    /**
     * Removes all quota records for the given expense. Called before a recalculation
     * so that old quotas are replaced cleanly.
     */
    @Modifying
    @Query("DELETE FROM CotaRateio c WHERE c.despesa.id = :despesaId")
    void deleteByDespesaId(@Param("despesaId") Long despesaId);
}
