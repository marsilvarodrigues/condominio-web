package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.condominio.model.Condominio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Condominio} entities, supporting specification-based filtering.
 */
@Repository
public interface CondominioRepository extends JpaRepository<Condominio, Long>, JpaSpecificationExecutor<Condominio> {

    /**
     * Looks up a non-deleted condominio by its unique CNPJ.
     *
     * @param cnpj the Brazilian company registration number (CNPJ)
     * @return an {@link Optional} containing the condominio if found
     */
    Optional<Condominio> findByCnpj(String cnpj);

    /**
     * Returns the IDs of all non-deleted condominios. Used by the rateio scheduler
     * to iterate over every active tenant.
     */
    @Query("SELECT c.id FROM Condominio c")
    List<Long> findAllIds();
}