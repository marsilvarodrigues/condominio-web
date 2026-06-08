package com.pmrodrigues.security.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned after a successful account activation.
 *
 * <p>Contains short-lived credentials for the now-enabled account and a {@code redirectUrl} that
 * the frontend should use to navigate the user to the change-password screen. The user must change
 * their temporary password before the session expires.
 */
public record ActivationResponseDTO(
    @JsonProperty("accessToken") String accessToken,
    @JsonProperty("refreshToken") String refreshToken,
    @JsonProperty("tokenType") String tokenType,
    @JsonProperty("expiresIn") long expiresIn,
    @JsonProperty("userId") Long userId,
    @JsonProperty("redirectUrl") String redirectUrl) {}
