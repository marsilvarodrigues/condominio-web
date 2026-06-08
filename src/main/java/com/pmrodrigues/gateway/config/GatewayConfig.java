package com.pmrodrigues.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers Asaas gateway properties as {@link
 * org.springframework.boot.context.properties.ConfigurationProperties} beans.
 */
@Configuration
@EnableConfigurationProperties(AsaasProperties.class)
public class GatewayConfig {}
