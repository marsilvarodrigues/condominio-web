package com.pmrodrigues.commons.tenant;

/**
 * Thread-local holder for the current tenant's condominio ID, used to enforce multi-tenancy isolation.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CONDOMINIO_ID = new ThreadLocal<>();

    private TenantContext() {}

    /**
     * Binds the given condominio ID to the current thread.
     *
     * @param id the tenant's condominio identifier
     */
    public static void setCondominioId(Long id) {
        CONDOMINIO_ID.set(id);
    }

    /**
     * Returns the condominio ID bound to the current thread, or {@code null} if none is set.
     *
     * @return current tenant ID
     */
    public static Long getCondominioId() {
        return CONDOMINIO_ID.get();
    }

    /**
     * Removes the condominio ID from the current thread to prevent leaks across request boundaries.
     */
    public static void clear() {
        CONDOMINIO_ID.remove();
    }
}
