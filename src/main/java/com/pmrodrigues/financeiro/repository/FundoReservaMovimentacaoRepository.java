package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.FundoReservaMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
