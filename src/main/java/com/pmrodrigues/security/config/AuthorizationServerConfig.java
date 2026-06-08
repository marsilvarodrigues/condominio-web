package com.pmrodrigues.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Configures the Spring Authorization Server with OAuth2/OIDC support, RSA-signed JWTs, and a
 * single registered client.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class AuthorizationServerConfig {

  private final JwtProperties jwtProperties;

  /**
   * Registers the authorization server security filter chain with OIDC enabled and bearer-token
   * error handling.
   *
   * @return the configured {@link SecurityFilterChain}
   * @throws Exception if the security configuration fails
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    var authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, server -> server.oidc(Customizer.withDefaults()))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new BearerTokenAuthenticationEntryPoint(),
                    new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON)));

    return http.build();
  }

  /**
   * Creates an in-memory {@link RegisteredClientRepository} configured with credentials and token
   * settings from {@link JwtProperties}.
   *
   * @return the registered client repository
   */
  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    var client =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(jwtProperties.getClientId())
            .clientSecret(jwtProperties.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri(jwtProperties.getRedirectUri())
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope("api")
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenTimeToLive(
                        Duration.ofSeconds(jwtProperties.getAccessTokenExpiration()))
                    .refreshTokenTimeToLive(
                        Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration()))
                    .reuseRefreshTokens(false)
                    .build())
            .build();

    return new InMemoryRegisteredClientRepository(client);
  }

  /**
   * Adds a {@code roles} claim to access tokens so they remain consistent with tokens issued by
   * {@code AuthController}.
   *
   * @return the token customizer
   */
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
      if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
        var roles =
            context.getPrincipal().getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        context.getClaims().claim("roles", roles);
      }
    };
  }

  /**
   * Exposes the RSA JWK set as a {@link JWKSource} for JWT signing and verification.
   *
   * @return the JWK source backed by a generated RSA key pair
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    return new ImmutableJWKSet<>(new JWKSet(generateRsaKey()));
  }

  /**
   * Configures the authorization server issuer URI from {@link JwtProperties}.
   *
   * @return the authorization server settings
   */
  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().issuer(jwtProperties.getIssuer()).build();
  }

  private static RSAKey generateRsaKey() {
    try {
      var generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      var keyPair = generator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
          .privateKey((RSAPrivateKey) keyPair.getPrivate())
          .keyID(UUID.randomUUID().toString())
          .build();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to generate RSA key pair", e);
    }
  }
}
