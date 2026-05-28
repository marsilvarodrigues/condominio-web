package com.pmrodrigues.commons.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables JPA auditing and exposes the currently authenticated username as the auditor.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * Provides the authenticated principal name for {@code @CreatedBy}/{@code @LastModifiedBy} fields.
     *
     * @return an {@link AuditorAware} that resolves the current Spring Security username
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(auth -> auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                .map(Authentication::getName);
    }
}