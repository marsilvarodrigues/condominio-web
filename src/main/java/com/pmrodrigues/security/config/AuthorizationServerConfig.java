package com.pmrodrigues.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
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
import org.springframework.util.StringUtils;

/**
 * Configures the Spring Authorization Server with OAuth2/OIDC support, RSA-signed JWTs, and a
 * single registered client.
 */
@Slf4j
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
    var encodedSecret =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()
            .encode(jwtProperties.getClientSecret());
    var client =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(jwtProperties.getClientId())
            .clientSecret(encodedSecret)
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
   * <p>When {@code security.jwt.rsa-private-key} and {@code security.jwt.rsa-public-key} are set
   * (via {@code RSA_PRIVATE_KEY} / {@code RSA_PUBLIC_KEY} env vars), the configured key pair is
   * loaded. Otherwise an ephemeral key is generated — suitable only for local development, since
   * tokens become invalid on restart and cannot be verified across instances.
   *
   * @return the JWK source backed by a persistent or ephemeral RSA key pair
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    return new ImmutableJWKSet<>(new JWKSet(loadOrGenerateRsaKey()));
  }

  private RSAKey loadOrGenerateRsaKey() {
    var privateKeyPem = jwtProperties.getRsaPrivateKey();
    var publicKeyPem = jwtProperties.getRsaPublicKey();
    boolean hasPrivate = StringUtils.hasText(privateKeyPem);
    boolean hasPublic = StringUtils.hasText(publicKeyPem);
    if (hasPrivate != hasPublic) {
      throw new IllegalStateException(
          "Both security.jwt.rsa-private-key and security.jwt.rsa-public-key must be configured together");
    }
    if (hasPrivate) {
      log.info("Loading RSA key pair from configuration");
      return buildRsaKey(parsePrivateKey(privateKeyPem), parsePublicKey(publicKeyPem));
    }
    log.warn(
        "RSA_PRIVATE_KEY / RSA_PUBLIC_KEY not set — generating ephemeral RSA key pair. "
            + "Tokens will be invalid after restart. Do NOT use in production.");
    return generateRsaKey();
  }

  private static RSAKey buildRsaKey(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
    return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("condominio-jwk").build();
  }

  private static RSAPrivateKey parsePrivateKey(String pem) {
    try {
      var stripped = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
      var bytes = Base64.getDecoder().decode(stripped);
      return (RSAPrivateKey) KeyFactory.getInstance("RSA")
          .generatePrivate(new PKCS8EncodedKeySpec(bytes));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse RSA private key from configuration", e);
    }
  }

  private static RSAPublicKey parsePublicKey(String pem) {
    try {
      var stripped = pem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", "");
      var bytes = Base64.getDecoder().decode(stripped);
      return (RSAPublicKey) KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(bytes));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse RSA public key from configuration", e);
    }
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
