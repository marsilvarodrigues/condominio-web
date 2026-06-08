package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.FundoReserva;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link FundoReserva} entities. */
@Repository
public interface FundoReservaRepository extends JpaRepository<FundoReserva, Long> {

  /**
   * Returns the single active FundoReserva for the current tenant (enforced by Hibernate filter).
   */
  Optional<FundoReserva> findFirstBy();

  /** Checks if any active FundoReserva exists for the current tenant. */
  boolean existsByDeletedFalse();

  /** Soft-deletes all FundoReserva belonging to the given condominio. */
  @Modifying
  @Query("UPDATE FundoReserva f SET f.deleted = true WHERE f.condominio.id = :condominioId")
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
