package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.LancamentoBancario;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for {@link LancamentoBancario} entities. */
@Repository
public interface LancamentoBancarioRepository
    extends JpaRepository<LancamentoBancario, Long>, JpaSpecificationExecutor<LancamentoBancario> {

  /** Returns all non-deleted lancamentos for a given conta bancaria and status. */
  List<LancamentoBancario> findByContaBancariaIdAndStatus(
      Long contaBancariaId, StatusLancamento status);

  /** Soft-deletes all lancamentos whose conta bancaria belongs to the given condominio. */
  @Modifying
  @Query(
      value =
          "UPDATE lancamentos_bancarios SET deleted = true "
              + "WHERE conta_bancaria_id IN "
              + "(SELECT id FROM contas_bancarias WHERE condominio_id = :condominioId) "
              + "AND deleted = false",
      nativeQuery = true)
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);
}
