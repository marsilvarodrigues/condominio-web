package com.pmrodrigues.security.repository;

import com.pmrodrigues.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds an active (non-deleted) user by email address.
     *
     * @return the matching user, or empty if none found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds an active user by their one-time activation token.
     *
     * @return the matching user, or empty if the token is unknown or already consumed
     */
    Optional<User> findByActivationToken(String activationToken);
}