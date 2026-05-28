package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for the token-refresh endpoint, carrying the opaque refresh token issued at login.
 */
public record RefreshRequestDTO(@NotBlank String refreshToken) {}
