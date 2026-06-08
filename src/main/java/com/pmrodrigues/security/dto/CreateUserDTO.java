package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

/**
 * DTO used exclusively for user creation requests; contains only the fields required to open a new
 * account.
 *
 * <p>{@code condominioIds} identifies the condominiums the user will have access to. An empty or
 * null set means the user will have global access to all data (master admin). {@code roles} must
 * contain at least one role. Email uniqueness is enforced at service level.
 */
public record CreateUserDTO(
    @Email @NotBlank String email,
    @NotBlank @NotNull String name,
    @NotNull @NotEmpty Set<String> roles,
    Set<Long> condominioIds) {}
