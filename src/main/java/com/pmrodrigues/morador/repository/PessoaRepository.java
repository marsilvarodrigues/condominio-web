package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.Pessoa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Pessoa} entities, supporting specification-based filtering.
 */
@Repository
public interface PessoaRepository
    extends JpaRepository<Pessoa, Long>, JpaSpecificationExecutor<Pessoa> {

  /**
   * Soft-deletes all pessoas belonging to the given condominio. Covers all SINGLE_TABLE
   * discriminators (PF, PJ, PROP_PF, PROP_PJ) in one query.
   *
   * @param condominioId condominio primary key
   */
  @Modifying
  @Query("UPDATE Pessoa p SET p.deleted = true WHERE p.condominio.id = :condominioId")
  void softDeleteByCondominioId(@Param("condominioId") Long condominioId);

  /**
   * Returns all active pessoas (moradores) assigned to the given apartment.
   *
   * @param apartamentoId apartment primary key
   * @return list of pessoas linked to this apartment
   */
  List<Pessoa> findByApartamentoId(Long apartamentoId);

  /**
   * Returns the pessoa whose user account has the given email.
   *
   * @param email user account email
   * @return the matching pessoa, or empty if no account with this email is a pessoa
   */
  @Query("SELECT p FROM Pessoa p WHERE p.email = :email")
  Optional<Pessoa> findByEmail(@Param("email") String email);
}
