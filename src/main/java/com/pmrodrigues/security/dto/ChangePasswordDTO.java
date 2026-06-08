package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user self-service password change.
 *
 * @param currentPassword the user's current password for identity verification
 * @param newPassword the new password to set; minimum 8 characters
 * @param confirmPassword must be identical to {@code newPassword}
 */
public record ChangePasswordDTO(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8) String newPassword,
    @NotBlank String confirmPassword) {}
