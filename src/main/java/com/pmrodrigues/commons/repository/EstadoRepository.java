package com.pmrodrigues.commons.repository;

import com.pmrodrigues.commons.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Estado} entities.
 */
@Repository
public interface EstadoRepository extends JpaRepository<Estado, Long>, JpaSpecificationExecutor<Estado> {
}
