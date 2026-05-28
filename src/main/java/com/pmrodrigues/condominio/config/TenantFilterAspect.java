package com.pmrodrigues.condominio.config;

import com.pmrodrigues.commons.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that activates the Hibernate {@code condominioFilter} with the current tenant's id before each repository method executes.
 */
@Slf4j
@Aspect
@Component
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Enables the {@code condominioFilter} Hibernate filter on the current session before any repository method runs; no-ops when no tenant is set.
     */
    @Before("execution(* com.pmrodrigues.condominio.repository.*.*(..))")
    public void enableCondominioFilter() {
        Long condominioId = TenantContext.getCondominioId();
        if (condominioId == null) {
            return;
        }
        log.debug("Enabling condominioFilter for condominioId={}", condominioId);
        entityManager.unwrap(Session.class)
                .enableFilter("condominioFilter")
                .setParameter("condominioId", condominioId);
    }
}
