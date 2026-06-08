package com.pmrodrigues.gateway.repository;

import com.pmrodrigues.gateway.model.AsaasCustomer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for cached Asaas customer ID mappings. */
public interface AsaasCustomerRepository extends JpaRepository<AsaasCustomer, Long> {

  /**
   * Returns the Asaas customer mapping for the given local Pessoa, if one has been cached.
   *
   * @param pessoaId local Pessoa primary key
   * @return the cached mapping, or empty if the resident has never been created in Asaas
   */
  Optional<AsaasCustomer> findByPessoaId(Long pessoaId);
}
