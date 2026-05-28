package com.pmrodrigues.condominio.repository;

import com.pmrodrigues.condominio.model.Apartamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Apartamento} entities, supporting specification-based filtering.
 */
@Repository
public interface ApartamentoRepository extends JpaRepository<Apartamento, Long>, JpaSpecificationExecutor<Apartamento> {
}
