package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.CoeficienteRateio;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link CoeficienteRateio} entities. */
@Repository
public interface CoeficienteRateioRepository extends JpaRepository<CoeficienteRateio, Long> {

  /**
   * Returns the most recent active coefficient records for each apartment in the group, using the
   * record whose {@code vigencia} is on or before the reference date.
   *
   * @param grupoDespesaId the group to query
   * @param referencia the date used to select the applicable version of each coefficient
   */
  @Query(
      """
      SELECT c FROM CoeficienteRateio c
      WHERE c.grupoDespesa.id = :grupoDespesaId
        AND c.vigencia = (
            SELECT MAX(c2.vigencia) FROM CoeficienteRateio c2
            WHERE c2.grupoDespesa.id = :grupoDespesaId
              AND c2.apartamento.id = c.apartamento.id
              AND c2.vigencia <= :referencia
              AND c2.deleted = false
        )
        AND c.deleted = false
      """)
  List<CoeficienteRateio> findVigentesPorGrupo(
      @Param("grupoDespesaId") Long grupoDespesaId, @Param("referencia") LocalDate referencia);

  /** Returns all active coefficient records for the given group, ordered by vigencia descending. */
  List<CoeficienteRateio> findByGrupoDespesaIdOrderByVigenciaDesc(Long grupoDespesaId);
}
