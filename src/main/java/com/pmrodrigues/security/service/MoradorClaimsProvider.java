package com.pmrodrigues.security.service;

import io.micrometer.core.annotation.Timed;
import java.util.Optional;

/**
 * Port that provides the JWT {@code apartamento_id} claim for a morador by email. Implemented in
 * the {@code morador} module to keep the {@code security} module free of morador-domain
 * dependencies.
 */
public interface MoradorClaimsProvider {

  /**
   * Returns the apartment id the morador whose user account has the given email is currently
   * assigned to, or empty if the user is not a morador or has no apartment assigned.
   *
   * @param email user account email
   * @return the apartment id, or empty
   */
  @Timed(value = "morador.claims.findApartamentoId", description = "Find morador apartamento_id by email")
  Optional<Long> findApartamentoIdByEmail(String email);
}
