package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.condominio.model.Bloco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Bloco} entities, supporting specification-based filtering.
 */
@Repository
public interface BlocoRepository extends JpaRepository<Bloco, Long>, JpaSpecificationExecutor<Bloco> {

    /**
     * Soft-deletes all blocos belonging to the given condominio by setting {@code deleted = true}.
     *
     * @param condominioId the condominio whose blocos should be soft-deleted
     */
    @Modifying
    @Query("UPDATE Bloco b SET b.deleted = true WHERE b.condominio.id = :condominioId")
    void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
