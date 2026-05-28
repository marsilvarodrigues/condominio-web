package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request carrying the user's credentials.
 */
public record AuthRequestDTO(@Email @NotBlank String email, @NotBlank String password) {}
