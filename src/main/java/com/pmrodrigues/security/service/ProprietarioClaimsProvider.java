package com.pmrodrigues.security.service;

import com.pmrodrigues.security.dto.ProprietarioClaims;
import io.micrometer.core.annotation.Timed;
import java.util.Optional;

/**
 * Port that provides JWT claim data for a proprietário by email. Implemented in the {@code morador}
 * module to keep the {@code security} module free of morador-domain dependencies.
 */
public interface ProprietarioClaimsProvider {

  /**
   * Returns the claims for the proprietário whose user account has the given email, or empty if the
   * user is not a proprietário.
   *
   * @param email user account email
   * @return proprietário claims, or empty
   */
  @Timed(value = "proprietario.claims.findByEmail", description = "Find proprietario claims by email")
  Optional<ProprietarioClaims> findClaimsByEmail(String email);
}
