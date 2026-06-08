package com.pmrodrigues.security.dto;

import lombok.Builder;

/**
 * Response payload returned after a successful login or token-refresh, containing both the access
 * token and the rotating refresh token.
 */
@Builder
public record AuthResponseDTO(
    String accessToken, String refreshToken, String tokenType, long expiresIn) {}
