package com.pmrodrigues.financeiro.repository;

import com.pmrodrigues.financeiro.model.GrupoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link GrupoDespesa} entities. */
@Repository
public interface GrupoDespesaRepository
    extends JpaRepository<GrupoDespesa, Long>, JpaSpecificationExecutor<GrupoDespesa> {}
