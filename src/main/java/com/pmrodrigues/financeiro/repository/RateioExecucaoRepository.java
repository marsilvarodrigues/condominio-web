package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.RateioExecucao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link RateioExecucao} audit records.
 */
@Repository
public interface RateioExecucaoRepository
        extends JpaRepository<RateioExecucao, Long>, JpaSpecificationExecutor<RateioExecucao> {

    /**
     * Returns all executions for a given expense, ordered by execution date descending.
     */
    Page<RateioExecucao> findByDespesaIdOrderByDataExecucaoDesc(Long despesaId, Pageable pageable);
}
