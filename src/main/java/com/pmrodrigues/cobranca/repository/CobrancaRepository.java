package com.pmrodrigues.cobranca.repository;

import com.pmrodrigues.cobranca.model.Cobranca;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Cobranca} entities.
 *
 * <p>Provides standard CRUD, specification-based filtering, and custom lookup methods
 * used by the billing service and webhook handler.
 */
@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long>,
        JpaSpecificationExecutor<Cobranca> {

    /**
     * Finds a charge by its originating CotaRateio ID.
     * Used for idempotency checks during charge generation.
     *
     * @param cotaRateioId CotaRateio primary key
     * @return optional charge, empty if none exists for this quota
     */
    Optional<Cobranca> findByCotaRateioId(Long cotaRateioId);

    /**
     * Returns all non-deleted charges for a given apartment and status.
     *
     * @param aptId  apartment primary key
     * @param status lifecycle status to filter by
     * @return list of matching charges
     */
    List<Cobranca> findByApartamentoIdAndStatus(Long aptId, StatusCobranca status);

    /**
     * Returns all charges with the given status where the billing email has not been sent.
     * Used by scheduled rematch / resend jobs.
     *
     * @param status lifecycle status
     * @return list of matching charges
     */
    List<Cobranca> findByStatusAndEmailEnviadoFalse(StatusCobranca status);

    /**
     * Returns all charges for a given apartment, ordered by creation date descending.
     *
     * @param apartamentoId apartment primary key
     * @param pageable      pagination parameters
     * @return page of charges
     */
    Page<Cobranca> findByApartamentoIdOrderByCreatedAtDesc(Long apartamentoId, Pageable pageable);

    /**
     * Looks up a charge by its Asaas payment ID using a native query.
     * Bypasses Hibernate's tenant filter (the WHERE clause includes {@code deleted = false}).
     *
     * @param asaasId Asaas payment ID
     * @return optional charge, empty if not found
     */
    @Query(value = "SELECT * FROM cobrancas WHERE asaas_id = :asaasId AND deleted = false LIMIT 1",
           nativeQuery = true)
    Optional<Cobranca> findByAsaasIdNative(@Param("asaasId") String asaasId);
}
