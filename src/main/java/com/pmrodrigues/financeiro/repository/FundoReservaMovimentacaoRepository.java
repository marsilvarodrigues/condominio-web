package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.FundoReservaMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link FundoReservaMovimentacao} entities, supporting paginated listing.
 */
@Repository
public interface FundoReservaMovimentacaoRepository extends JpaRepository<FundoReservaMovimentacao, Long> {

    /**
     * Returns paginated movements for a given fundo reserva, most recent first.
     */
    Page<FundoReservaMovimentacao> findByFundoReservaIdOrderByDataMovimentacaoDesc(Long fundoReservaId, Pageable pageable);

    /**
     * Soft-deletes all movements belonging to the given condominio.
     */
    @Modifying
    @Query("UPDATE FundoReservaMovimentacao m SET m.deleted = true WHERE m.condominio.id = :condominioId")
    void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
