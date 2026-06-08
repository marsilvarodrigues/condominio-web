package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.ItemExtrato;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link ItemExtrato} entities. */
@Repository
public interface ItemExtratoRepository extends JpaRepository<ItemExtrato, Long> {

  /**
   * Returns all statement items associated with the given budget line item.
   *
   * @param itemOrcamentoId the target budget line item identifier
   */
  @Query("SELECT ie FROM ItemExtrato ie WHERE ie.itemOrcamento.id = :itemOrcamentoId")
  List<ItemExtrato> findByItemOrcamentoId(@Param("itemOrcamentoId") Long itemOrcamentoId);
}
