package com.pmrodrigues.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Asaas payment gateway.
 *
 * <p>Bound to the {@code app.cobranca.asaas} prefix from {@code application.yaml}.
 *
 * @param baseUrl Asaas REST API base URL (sandbox or production)
 * @param apiKey Asaas account API key (access_token header value)
 * @param walletId Asaas wallet identifier for split payment routing (optional)
 */
@ConfigurationProperties(prefix = "app.cobranca.asaas")
public record AsaasProperties(String baseUrl, String apiKey, String walletId) {}
