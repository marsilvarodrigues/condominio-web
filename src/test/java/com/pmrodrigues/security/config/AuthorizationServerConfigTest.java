package com.pmrodrigues.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServerConfigTest {

    private JwtProperties jwtProperties;
    private AuthorizationServerConfig config;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setClientId("test-client");
        jwtProperties.setClientSecret("test-secret");
        config = new AuthorizationServerConfig(jwtProperties);
    }

    @Test
    void jwkSource_whenKeysNotConfigured_returnsEphemeralSource() {
        assertThat(config.jwkSource()).isNotNull();
    }

    @Test
    void jwkSource_whenBothKeysConfigured_loadsFromProperties() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var kp = gen.generateKeyPair();
        jwtProperties.setRsaPrivateKey(toPem("PRIVATE KEY", kp.getPrivate().getEncoded()));
        jwtProperties.setRsaPublicKey(toPem("PUBLIC KEY", kp.getPublic().getEncoded()));

        assertThat(config.jwkSource()).isNotNull();
    }

    @Test
    void jwkSource_whenOnlyPrivateKeySet_throwsIllegalState() {
        jwtProperties.setRsaPrivateKey("-----BEGIN PRIVATE KEY-----\nfake\n-----END PRIVATE KEY-----");

        assertThatThrownBy(() -> config.jwkSource())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be configured together");
    }

    @Test
    void jwkSource_whenOnlyPublicKeySet_throwsIllegalState() {
        jwtProperties.setRsaPublicKey("-----BEGIN PUBLIC KEY-----\nfake\n-----END PUBLIC KEY-----");

        assertThatThrownBy(() -> config.jwkSource())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be configured together");
    }

    @Test
    void jwkSource_whenInvalidPrivateKeyPem_throwsIllegalState() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        var kp = gen.generateKeyPair();
        jwtProperties.setRsaPrivateKey("-----BEGIN PRIVATE KEY-----\nNOTVALIDBASE64!!!\n-----END PRIVATE KEY-----");
        jwtProperties.setRsaPublicKey(toPem("PUBLIC KEY", kp.getPublic().getEncoded()));

        assertThatThrownBy(() -> config.jwkSource())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to parse RSA private key");
    }

    @Test
    void registeredClientRepository_encodesSecretWithDelegatingEncoder() {
        var repo = config.registeredClientRepository();
        var client = repo.findByClientId("test-client");

        assertThat(client).isNotNull();
        assertThat(client.getClientSecret()).doesNotContain("test-secret");
        assertThat(client.getClientSecret()).matches("\\{[a-z0-9]+\\}.+");
    }

    @Test
    void registeredClientRepository_clientIdMatchesProperties() {
        var repo = config.registeredClientRepository();
        assertThat(repo.findByClientId("test-client")).isNotNull();
        assertThat(repo.findByClientId("other-client")).isNull();
    }

    private static String toPem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n" +
            Base64.getEncoder().encodeToString(encoded) +
            "\n-----END " + type + "-----";
    }
}
