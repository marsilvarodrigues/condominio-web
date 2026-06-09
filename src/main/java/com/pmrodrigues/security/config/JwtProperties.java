package com.pmrodrigues.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration properties for JWT issuance bound to the {@code security.jwt} prefix.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

  @Positive private long accessTokenExpiration = 3600;

  @Positive private long refreshTokenExpiration = 86400;

  @NotBlank private String issuer = "condominio";

  @NotBlank private String clientId;

  @NotBlank private String clientSecret;

  @NotBlank private String redirectUri = "http://localhost:8080/login/oauth2/code/condominio";

  /** PEM-encoded PKCS#8 RSA private key. When blank, an ephemeral key is generated (dev only). */
  private String rsaPrivateKey;

  /** PEM-encoded X.509 RSA public key. Must be set together with {@code rsaPrivateKey}. */
  private String rsaPublicKey;
}
