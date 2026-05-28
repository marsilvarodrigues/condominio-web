package com.pmrodrigues.security.specification;

import com.pmrodrigues.security.model.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Factory class providing JPA {@link Specification} predicates for querying {@link User} entities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSpecification {

    /**
     * Returns a specification that matches users whose name contains the given substring (case-insensitive), or {@code null} when the filter is absent.
     */
    public static Specification<User> hasNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + nome.toLowerCase() + "%");
    }

    /**
     * Returns a specification that matches users whose email contains the given substring (case-insensitive), or {@code null} when the filter is absent.
     */
    public static Specification<User> hasEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    /**
     * Returns a specification that matches users by their enabled status, or {@code null} when the filter is absent.
     */
    public static Specification<User> hasEnabled(Boolean enabled) {
        if (enabled == null) return null;
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    /**
     * Returns a specification that matches users who have the given role, or {@code null} when the filter is absent.
     */
    public static Specification<User> hasRole(String role) {
        if (role == null || role.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.join("roles"), role);
    }
}
