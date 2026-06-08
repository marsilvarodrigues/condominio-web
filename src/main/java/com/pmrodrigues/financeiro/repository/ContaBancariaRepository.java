package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.ContaBancaria;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ContaBancaria} entities. */
@Repository
public interface ContaBancariaRepository
    extends JpaRepository<ContaBancaria, Long>, JpaSpecificationExecutor<ContaBancaria> {

  /** Returns true if any non-deleted account of the given type exists for the current tenant. */
  boolean existsByTipoAndDeletedFalse(TipoContaBancaria tipo);

  /** Returns the single FUNDO_RESERVA account for the current tenant, if any. */
  Optional<ContaBancaria> findByTipoAndDeletedFalse(TipoContaBancaria tipo);

  /** Soft-deletes all accounts belonging to the given condominio. */
  @Modifying
  @Query("UPDATE ContaBancaria c SET c.deleted = true WHERE c.condominio.id = :condominioId")
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
