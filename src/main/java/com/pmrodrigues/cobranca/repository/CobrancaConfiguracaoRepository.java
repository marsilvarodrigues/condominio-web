package com.pmrodrigues.cobranca.repository;

import com.pmrodrigues.cobranca.model.CobrancaConfiguracao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for per-condominium billing configuration. */
public interface CobrancaConfiguracaoRepository extends JpaRepository<CobrancaConfiguracao, Long> {

  /**
   * Returns the billing configuration for the given condominium, if one exists.
   *
   * @param condominioId condominium primary key
   * @return the configuration, or empty if the tenant has not customised the defaults
   */
  Optional<CobrancaConfiguracao> findByCondominioId(Long condominioId);
}
