package com.pmrodrigues.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating mutable fields of an existing {@link
 * com.pmrodrigues.financeiro.model.ContaBancaria}.
 */
public record UpdateContaBancariaDTO(
    @NotBlank @Size(max = 10) String agencia,
    @NotBlank @Size(max = 20) String conta,
    @Size(max = 2) String digito,
    @Size(max = 255) String descricao,
    @Size(max = 255) String chavePix) {}
