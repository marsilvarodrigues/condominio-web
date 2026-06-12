package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Payload for creating a new apartment unit, referencing the parent bloco by FK identifier. */
public record CreateApartamentoDTO(
    @NotNull Long blocoId,
    @NotBlank String numero,
    @NotNull BigDecimal areaConstruida,
    BigDecimal fracaoIdeal,
    Integer andar) {}
