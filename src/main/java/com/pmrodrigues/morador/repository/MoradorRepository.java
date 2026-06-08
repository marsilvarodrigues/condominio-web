package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.Morador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link Morador} entities. */
@Repository
public interface MoradorRepository
    extends JpaRepository<Morador, Long>, JpaSpecificationExecutor<Morador> {}
