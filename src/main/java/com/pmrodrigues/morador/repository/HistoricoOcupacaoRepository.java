package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.HistoricoOcupacao;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence port for {@link HistoricoOcupacao} — append-only, no soft-delete. */
@Repository
public interface HistoricoOcupacaoRepository extends JpaRepository<HistoricoOcupacao, Long> {

  /**
   * Returns all history records for a given apartment, ordered by data_saida descending (most
   * recent departure first).
   *
   * @param apartamentoId apartment primary key
   * @param condominioId tenant identifier
   * @return ordered list of occupancy records; never {@code null}
   */
  Page<HistoricoOcupacao> findByApartamentoIdAndCondominioIdOrderByDataSaidaDesc(
      Long apartamentoId, Long condominioId,
      Pageable pageable);
}
