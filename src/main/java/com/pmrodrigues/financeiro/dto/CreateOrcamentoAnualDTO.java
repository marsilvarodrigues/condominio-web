package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Payload for creating a new OrcamentoAnual (always starts as RASCUNHO). */
public record CreateOrcamentoAnualDTO(@NotNull @Min(2000) @Max(2100) Integer exercicio) {}
