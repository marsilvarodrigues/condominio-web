package com.pmrodrigues.commons.repository;

import com.pmrodrigues.commons.model.Banco;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link Banco} entities. */
@Repository
public interface BancoRepository
    extends JpaRepository<Banco, Long>, JpaSpecificationExecutor<Banco> {

  /** Looks up a banco by its FEBRABAN code. */
  Optional<Banco> findByCodigo(String codigo);
}
