package com.pmrodrigues.security.service;

import com.pmrodrigues.security.config.JwtProperties;
import com.pmrodrigues.security.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Generates RS256-signed JWT access tokens (with {@code roles} and {@code condominio_ids} claims)
 * and opaque UUID refresh tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;
  private final UserRepository userRepository;
  private final ProprietarioClaimsProvider proprietarioClaimsProvider;

  /**
   * Builds and signs an RS256 JWT access token containing the user's roles and their {@code
   * condominio_ids} list. An empty list means the user has global access (no tenant restriction).
   *
   * @param authentication the authenticated principal whose name and authorities are embedded in
   *     the token
   * @return the signed JWT string
   */
  @Timed(value = "jwt.service.generateAccessToken", description = "Generate JWT access token")
  public String generateAccessToken(Authentication authentication) {
    log.info("Generating access token for user: {}", authentication.getName());

    var now = Instant.now();
    var roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

    var user = userRepository.findByEmail(authentication.getName());
    Long userId = user.map(u -> u.getId()).orElse(null);
    List<Long> condominioIds =
        user
            .map(u -> u.getCondominios().stream().map(c -> c.getId()).collect(Collectors.toList()))
            .orElse(List.of());

    boolean isProprietario = roles.contains("ROLE_PROPRIETARIO");

    List<Long> apartamentosIdsProprietario =
        isProprietario
            ? proprietarioClaimsProvider
                .findClaimsByEmail(authentication.getName())
                .map(c -> c.apartamentoIds())
                .orElse(List.of())
            : List.of();

    var builder =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(jwtProperties.getAccessTokenExpiration()))
            .subject(authentication.getName())
            .id(UUID.randomUUID().toString())
            .claim("roles", roles)
            .claim("condominio_ids", condominioIds)
            .claim("apartamentos_ids_proprietario", apartamentosIdsProprietario);
    if (userId != null) {
      builder.claim("user_id", userId);
    }
    var claims = builder.build();

    var token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    log.info(
        "Access token generated for user: {} condominioIds: {} userId: {} ",
        authentication.getName(),
        condominioIds,
        userId);
    return token;
  }

  /**
   * Generates an opaque UUID refresh token.
   *
   * @return a random UUID string suitable for use as a refresh token
   */
  @Timed(value = "jwt.service.generateRefreshToken", description = "Generate JWT refresh token")
  public String generateRefreshToken() {
    log.info("Generating refresh token");
    var token = UUID.randomUUID().toString();
    log.info("Refresh token generated successfully");
    return token;
  }
}
