package com.pmrodrigues.morador.service;

import com.pmrodrigues.morador.repository.PessoaRepository;
import com.pmrodrigues.security.service.MoradorClaimsProvider;
import io.micrometer.core.annotation.Timed;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Morador-module implementation of {@link MoradorClaimsProvider}. Reads pessoa data via {@link
 * PessoaRepository} so the security module does not depend on morador repositories.
 */
@Service
@RequiredArgsConstructor
public class MoradorClaimsProviderImpl implements MoradorClaimsProvider {

  private final PessoaRepository pessoaRepository;

  /**
   * Called from {@code JwtService.generateAccessToken} during login, with no ambient transaction
   * of its own — {@code @Transactional} keeps the session open while {@code apartamento} (a lazy
   * {@code @ManyToOne}) is read.
   */
  @Override
  @Transactional(readOnly = true)
  @Timed(value = "morador.claims.findApartamentoId", description = "Find morador apartamento_id by email")
  public Optional<Long> findApartamentoIdByEmail(String email) {
    return pessoaRepository
        .findByEmail(email)
        .map(p -> p.getApartamento())
        .map(a -> a.getId());
  }
}
