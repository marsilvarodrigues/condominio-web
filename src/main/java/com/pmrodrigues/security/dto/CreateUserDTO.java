package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * DTO used exclusively for user creation requests; contains only the fields required to open a new account.
 *
 * <p>{@code condominioId} identifies the condominium the user will belong to and must reference an existing record.
 * {@code roles} must contain at least one role. Email uniqueness is enforced at service level.
 */
public record CreateUserDTO(
        @Email
        @NotBlank
        String email,
        @NotBlank
        @NotNull
        String name,
        @NotNull
        @NotEmpty
        Set<String> roles,
        @NotNull
        @Min(1)
        Long condominioId
) {
}