package com.pmrodrigues.commons.config;

import com.pmrodrigues.commons.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that activates the Hibernate {@code condominioFilter} with the current tenant id
 * before any repository method in any module executes.
 *
 * <p>The pointcut covers all packages under {@code com.pmrodrigues}, so new modules automatically
 * receive tenant filtering without additional aspects. Entities that do not declare
 * {@code @Filter(name="condominioFilter")} are unaffected — Hibernate silently skips filters that
 * are not registered on a given entity.
 */
@Slf4j
@Aspect
@Component
public class TenantFilterAspect {

  public static final String CONDOMINIO_FILTER = "condominioFilter";
  @PersistenceContext private EntityManager entityManager;

  /**
   * Enables the {@code condominioFilter} on the current Hibernate session before any repository
   * method runs; no-ops when no tenant is set in {@link TenantContext}.
   */
  @Before("execution(* com.pmrodrigues..repository.*.*(..))")
  public void enableCondominioFilter() {
    Long condominioId = TenantContext.getCondominioId();
    if (condominioId == null) {
      return;
    }
    log.debug("Enabling condominioFilter for condominioId={}", condominioId);
    entityManager
        .unwrap(Session.class)
        .enableFilter(CONDOMINIO_FILTER)
        .setParameter("condominioId", condominioId);
  }
}
