package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.FundoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link FundoReserva} entities.
 */
@Repository
public interface FundoReservaRepository extends JpaRepository<FundoReserva, Long> {

    /**
     * Returns the single active FundoReserva for the current tenant (enforced by Hibernate filter).
     */
    Optional<FundoReserva> findFirstBy();

    /**
     * Checks if any active FundoReserva exists for the current tenant.
     */
    boolean existsByDeletedFalse();
}
