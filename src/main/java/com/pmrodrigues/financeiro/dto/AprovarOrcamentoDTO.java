package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for approving an OrcamentoAnual; the caller provides the number of active units
 * so the service can compute the estimated monthly fee without cross-module repository access.
 */
public record AprovarOrcamentoDTO(
        @NotNull @Min(1) Integer numeroUnidades
) {
}
