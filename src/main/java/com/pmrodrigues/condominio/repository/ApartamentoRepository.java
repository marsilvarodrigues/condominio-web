package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.condominio.model.Apartamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Apartamento} entities, supporting specification-based filtering.
 */
@Repository
public interface ApartamentoRepository extends JpaRepository<Apartamento, Long>, JpaSpecificationExecutor<Apartamento> {

    /**
     * Soft-deletes all apartamentos belonging to the given condominio by setting {@code deleted = true}.
     *
     * @param condominioId the condominio whose apartamentos should be soft-deleted
     */
    @Modifying
    @Query("UPDATE Apartamento a SET a.deleted = true WHERE a.condominio.id = :condominioId")
    void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
