package com.pmrodrigues.morador.service;

import com.pmrodrigues.morador.repository.ProprietarioRepository;
import com.pmrodrigues.security.dto.ProprietarioClaims;
import com.pmrodrigues.security.service.ProprietarioClaimsProvider;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Morador-module implementation of {@link ProprietarioClaimsProvider}. Reads proprietário data via
 * {@link ProprietarioRepository} so the security module does not depend on morador repositories.
 */
@Service
@RequiredArgsConstructor
public class ProprietarioClaimsProviderImpl implements ProprietarioClaimsProvider {

  private final ProprietarioRepository proprietarioRepository;

  @Override
  @Timed(value = "proprietario.claims.findByEmail", description = "Find proprietario claims by email")
  public Optional<ProprietarioClaims> findClaimsByEmail(String email) {
    return proprietarioRepository
        .findByUserEmail(email)
        .map(
            p ->
                new ProprietarioClaims(
                    p.getId(),
                    p.getApartamentos().stream()
                        .map(a -> a.getId())
                        .collect(Collectors.toList())));
  }
}
