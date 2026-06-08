package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.PlanoContas;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link PlanoContas} entities, with tree-root lookup and
 * specification support.
 */
@Repository
public interface PlanoContasRepository
    extends JpaRepository<PlanoContas, Long>, JpaSpecificationExecutor<PlanoContas> {

  /** Returns all root nodes (nodes without a parent) for the current tenant. */
  List<PlanoContas> findAllByPaiIsNull();

  /** Returns whether any child nodes exist for the given parent id. */
  boolean existsByPaiId(Long paiId);

  /** Soft-deletes all PlanoContas belonging to the given condominio. */
  @Modifying
  @Query("UPDATE PlanoContas p SET p.deleted = true WHERE p.condominio.id = :condominioId")
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
