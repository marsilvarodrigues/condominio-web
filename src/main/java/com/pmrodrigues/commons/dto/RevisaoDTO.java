package com.pmrodrigues.commons.dto;

import java.util.Set;

/**
 * Immutable snapshot of a single Envers revision for a whitelisted audited entity.
 *
 * @param revision        Envers revision number
 * @param revisionType    ADD, MOD or DEL
 * @param timestamp       epoch-millis when the change was committed
 * @param username        authenticated user who triggered the change, or {@code null} for system events
 * @param snapshot        the entity state at this revision
 * @param camposAlterados names of fields that changed in this revision
 */
public record RevisaoDTO(
    int revision,
    String revisionType,
    long timestamp,
    String username,
    Object snapshot,
    Set<String> camposAlterados
) {}
