package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Represents a full apartment unit for read and update operations, including audit timestamps.
 */
public record ApartamentoDTO(
        Long id,

        Long blocoId,
        @NotBlank String numero,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
