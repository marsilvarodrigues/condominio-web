package com.pmrodrigues.cobranca.repository;

import com.pmrodrigues.cobranca.model.CobrancaConfiguracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for per-condominium billing configuration.
 */
public interface CobrancaConfiguracaoRepository extends JpaRepository<CobrancaConfiguracao, Long> {

    /**
     * Returns the billing configuration for the given condominium, if one exists.
     *
     * @param condominioId condominium primary key
     * @return the configuration, or empty if the tenant has not customised the defaults
     */
    Optional<CobrancaConfiguracao> findByCondominioId(Long condominioId);
}
