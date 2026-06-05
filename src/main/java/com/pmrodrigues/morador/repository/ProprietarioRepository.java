package com.pmrodrigues.morador.repository;

import com.pmrodrigues.morador.model.Proprietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Proprietario} entities, supporting specification-based filtering.
 */
@Repository
public interface ProprietarioRepository extends JpaRepository<Proprietario, Long>,
        JpaSpecificationExecutor<Proprietario> {

    /**
     * Returns all proprietarios associated with the given apartment.
     *
     * @param apartamentoId apartment primary key
     * @return list of proprietarios that own the apartment
     */
    List<Proprietario> findByApartamentosId(Long apartamentoId);
}
