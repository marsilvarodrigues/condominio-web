package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ItemOrcamento} entities.
 */
@Repository
public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {

    /**
     * Returns all items for a given budget (tenant-scoped via Hibernate filter).
     */
    List<ItemOrcamento> findByOrcamentoAnualId(Long orcamentoAnualId);

    /**
     * Soft-deletes all items belonging to the given OrcamentoAnual.
     */
    @Modifying
    @Query("UPDATE ItemOrcamento i SET i.deleted = true WHERE i.orcamentoAnual.id = :orcamentoId")
    void softDeleteByOrcamentoAnualId(@Param("orcamentoId") Long orcamentoId);
}
