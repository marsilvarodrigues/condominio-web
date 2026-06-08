package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Represents a full apartment unit for read and update operations, including audit timestamps. */
public record ApartamentoDTO(
    Long id,
    Long blocoId,
    @NotBlank String numero,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    @NotNull BigDecimal areaConstruida) {}
