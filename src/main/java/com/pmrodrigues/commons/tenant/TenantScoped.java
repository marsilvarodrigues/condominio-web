package com.pmrodrigues.commons.tenant;

/**
 * Implemented by every entity partitioned by {@code condominio_id}. Lets {@code
 * TenantFilterAspect} validate, after a primary-key lookup, that the fetched row actually belongs
 * to the active tenant — Hibernate's {@code @Filter} only restricts query-based fetches, not
 * {@code findById}/{@code get()}-style lookups by primary key, so without this check a row from
 * another condominio is returned as-is.
 */
public interface TenantScoped {

  /**
   * Returns the id of the condominio this row belongs to, or {@code null} if not yet assigned
   * (e.g. a transient instance that has not gone through {@code @PrePersist} yet).
   */
  Long getCondominioId();
}
