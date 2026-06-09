package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.Proprietario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Proprietario} entities, supporting specification-based
 * filtering.
 */
@Repository
public interface ProprietarioRepository
    extends JpaRepository<Proprietario, Long>, JpaSpecificationExecutor<Proprietario> {

  /**
   * Returns all proprietarios associated with the given apartment.
   *
   * @param apartamentoId apartment primary key
   * @return list of proprietarios that own the apartment
   */
  List<Proprietario> findByApartamentosId(Long apartamentoId);

  /**
   * Finds the proprietario whose underlying user account has the given email. Uses JPQL so
   * Hibernate generates the correct JOINED-inheritance joins across users/pessoas/proprietarios.
   * The {@code @SQLRestriction("deleted = false")} on {@link com.pmrodrigues.morador.model.Pessoa}
   * is applied automatically.
   *
   * @param email user account email
   * @return the proprietario, or empty if none exists
   */
  @Query("SELECT p FROM Proprietario p WHERE p.email = :email")
  Optional<Proprietario> findByUserEmail(@Param("email") String email);
}
