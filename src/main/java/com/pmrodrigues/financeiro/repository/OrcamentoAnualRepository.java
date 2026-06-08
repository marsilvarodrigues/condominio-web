package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.OrcamentoAnual;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link OrcamentoAnual} entities with specification support. */
@Repository
public interface OrcamentoAnualRepository
    extends JpaRepository<OrcamentoAnual, Long>, JpaSpecificationExecutor<OrcamentoAnual> {

  /**
   * Checks if an approved budget already exists for the given exercise year (tenant-scoped via
   * Hibernate filter).
   */
  boolean existsByExercicioAndStatus(Integer exercicio, StatusOrcamento status);

  /** Finds an approved budget for the given exercise year (tenant-scoped). */
  Optional<OrcamentoAnual> findByExercicioAndStatus(Integer exercicio, StatusOrcamento status);

  /** Soft-deletes all OrcamentoAnual belonging to the given condominio. */
  @Modifying
  @Query("UPDATE OrcamentoAnual o SET o.deleted = true WHERE o.condominio.id = :condominioId")
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
