package com.pmrodrigues.security.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.pmrodrigues.security.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

/**
 * Provides {@link JwtDecoder} and {@link JwtEncoder} beans, extending the default decoder with
 * token-blacklist enforcement.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

  private final JWKSource<SecurityContext> jwkSource;
  private final TokenBlacklistService tokenBlacklistService;

  /**
   * Overrides the default {@link JwtDecoder} to reject tokens that have been revoked via {@link
   * TokenBlacklistService}.
   *
   * @return a decoder that delegates to the standard Nimbus decoder then checks the blacklist
   * @throws BadJwtException if the token has been blacklisted
   */
  @Bean
  @Primary
  public JwtDecoder jwtDecoder() {
    var delegate = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    return token -> {
      var jwt = delegate.decode(token);
      if (tokenBlacklistService.isBlacklisted(jwt.getId())) {
        throw new BadJwtException("Token has been revoked");
      }
      return jwt;
    };
  }

  /**
   * Creates a {@link JwtEncoder} backed by the application's RSA JWK source.
   *
   * @return the Nimbus-based JWT encoder
   */
  @Bean
  public JwtEncoder jwtEncoder() {
    return new NimbusJwtEncoder(jwkSource);
  }
}
