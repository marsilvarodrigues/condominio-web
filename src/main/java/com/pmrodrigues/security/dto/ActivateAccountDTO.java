package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the account activation endpoint.
 *
 * <p>The activation token is the one-time UUID sent in the account-activation email. On success the
 * account is enabled and the response contains short-lived credentials that allow the user to
 * change their password immediately.
 */
public record ActivateAccountDTO(@NotBlank String activationToken) {}
