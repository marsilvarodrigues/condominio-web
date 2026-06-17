package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.HistoricoOcupacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence port for {@link HistoricoOcupacao} — append-only, no soft-delete. */
@Repository
public interface HistoricoOcupacaoRepository extends JpaRepository<HistoricoOcupacao, Long> {

  /**
   * Returns all history records for a given apartment, ordered by data_saida descending (most
   * recent departure first). JOIN FETCH on {@code pessoa} eliminates the N+1 that would otherwise
   * occur when the mapper accesses person fields during DTO conversion.
   *
   * @param apartamentoId apartment primary key
   * @param condominioId tenant identifier
   * @return ordered page of occupancy records; never {@code null}
   */
  @Query(
      value =
          "SELECT h FROM HistoricoOcupacao h JOIN FETCH h.pessoa "
              + "WHERE h.apartamento.id = :apartamentoId AND h.condominio.id = :condominioId "
              + "ORDER BY h.dataSaida DESC",
      countQuery =
          "SELECT COUNT(h) FROM HistoricoOcupacao h "
              + "WHERE h.apartamento.id = :apartamentoId AND h.condominio.id = :condominioId")
  Page<HistoricoOcupacao> findByApartamento_IdAndCondominio_IdOrderByDataSaidaDesc(
      @Param("apartamentoId") Long apartamentoId,
      @Param("condominioId") Long condominioId,
      Pageable pageable);
}
