package com.pmrodrigues.commons.config;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.commons.tenant.TenantScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
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

  /**
   * Safety net for primary-key lookups: Hibernate's {@code @Filter} (enabled above) only
   * restricts query-based fetches — {@code findById}/{@code get()}-style lookups by id bypass it
   * entirely and return the row regardless of tenant. This advice re-checks the fetched entity's
   * {@code condominio_id} against the active tenant and substitutes {@link Optional#empty()} when
   * they don't match, so callers see the same "not found" outcome they'd get for any other id that
   * isn't theirs.
   *
   * <p>No-ops when no tenant is active (global access) or the entity isn't {@link TenantScoped}.
   */
  @Around("execution(* com.pmrodrigues..repository.*.findById(..))")
  public Object enforceTenantOnFindById(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();

    Long activeCondominioId = TenantContext.getCondominioId();
    if (activeCondominioId == null || !(result instanceof Optional<?> optional) || optional.isEmpty()) {
      return result;
    }

    Object entity = optional.get();
    if (entity instanceof TenantScoped scoped) {
      Long entityCondominioId = scoped.getCondominioId();
      if (entityCondominioId != null && !entityCondominioId.equals(activeCondominioId)) {
        log.warn(
            "Blocked cross-tenant findById: entity condominioId={} activeCondominioId={} entityType={}",
            entityCondominioId,
            activeCondominioId,
            entity.getClass().getSimpleName());
        return Optional.empty();
      }
    }
    return result;
  }
}
