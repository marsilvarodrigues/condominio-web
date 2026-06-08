package com.pmrodrigues.commons.tenant;

import java.util.List;

/**
 * Thread-local holder for the current tenant's condominio ID and the user's allowed condominio IDs,
 * used to enforce multi-tenancy isolation.
 */
public final class TenantContext {

  private static final ThreadLocal<Long> CONDOMINIO_ID = new ThreadLocal<>();
  private static final ThreadLocal<List<Long>> ALLOWED_IDS = new ThreadLocal<>();

  private TenantContext() {}

  /**
   * Binds the given condominio ID to the current thread as the active tenant.
   *
   * @param id the active tenant's condominio identifier, or {@code null} for global access
   */
  public static void setCondominioId(Long id) {
    CONDOMINIO_ID.set(id);
  }

  /**
   * Returns the active condominio ID bound to the current thread, or {@code null} if none is set
   * (global access).
   *
   * @return current active tenant ID
   */
  public static Long getCondominioId() {
    return CONDOMINIO_ID.get();
  }

  /**
   * Binds the list of condominio IDs the current user is allowed to access. An empty list means
   * global access to all condominios.
   *
   * @param ids the allowed condominio identifiers from the JWT claim
   */
  public static void setAllowedCondominioIds(List<Long> ids) {
    ALLOWED_IDS.set(ids);
  }

  /**
   * Returns the list of condominio IDs the current user may access. Returns an empty list if no
   * restrictions are set (global access).
   *
   * @return allowed condominio IDs, never {@code null}
   */
  public static List<Long> getAllowedCondominioIds() {
    var ids = ALLOWED_IDS.get();
    return ids != null ? ids : List.of();
  }

  /**
   * Removes all tenant context from the current thread to prevent leaks across request boundaries.
   */
  public static void clear() {
    CONDOMINIO_ID.remove();
    ALLOWED_IDS.remove();
  }
}
