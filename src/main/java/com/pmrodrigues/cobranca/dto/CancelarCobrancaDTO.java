package com.pmrodrigues.cobranca.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for cancelling a charge.
 *
 * @param motivo human-readable reason for cancellation, stored in logs
 */
public record CancelarCobrancaDTO(@NotBlank String motivo) {}
