package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Represents a full building block for read and update operations, including audit timestamps.
 */
public record BlocoDTO(
        Long id,
        @NotNull Integer numero,
        @NotBlank String bloco,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
