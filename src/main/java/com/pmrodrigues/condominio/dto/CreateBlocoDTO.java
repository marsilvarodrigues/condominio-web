package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for creating a new building block within a condominium.
 */
public record CreateBlocoDTO(
        @NotNull Integer numero,
        @NotBlank String bloco
) {
}
