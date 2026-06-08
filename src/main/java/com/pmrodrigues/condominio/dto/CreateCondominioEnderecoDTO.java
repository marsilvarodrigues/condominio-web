package com.pmrodrigues.condominio.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Address payload for condominium creation requests. Mirrors {@link
 * com.pmrodrigues.commons.dto.EnderecoDTO} but accepts the state as a numeric identifier instead of
 * a full nested object, avoiding unnecessary round-trips for well-known reference data.
 */
public record CreateCondominioEnderecoDTO(
    String logradouro,
    @NotBlank @Size(min = 8, max = 8) String cep,
    String cidade,
    @NotNull @Min(1) Long estado) {}
