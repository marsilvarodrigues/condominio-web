package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link OrcamentoAnual} entities with specification support.
 */
@Repository
public interface OrcamentoAnualRepository extends JpaRepository<OrcamentoAnual, Long>, JpaSpecificationExecutor<OrcamentoAnual> {

    /**
     * Checks if an approved budget already exists for the given exercise year (tenant-scoped via Hibernate filter).
     */
    boolean existsByExercicioAndStatus(Integer exercicio, StatusOrcamento status);

    /**
     * Finds an approved budget for the given exercise year (tenant-scoped).
     */
    Optional<OrcamentoAnual> findByExercicioAndStatus(Integer exercicio, StatusOrcamento status);
}
