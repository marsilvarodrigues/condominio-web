package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.condominio.model.Bloco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Bloco} entities, supporting specification-based filtering.
 */
@Repository
public interface BlocoRepository extends JpaRepository<Bloco, Long>, JpaSpecificationExecutor<Bloco> {
}
