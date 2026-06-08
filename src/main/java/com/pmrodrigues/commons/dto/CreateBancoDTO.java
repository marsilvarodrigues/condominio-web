package com.pmrodrigues.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating a new {@link com.pmrodrigues.commons.model.Banco}. */
public record CreateBancoDTO(
    @NotBlank @Size(max = 10) String codigo,
    @NotBlank @Size(max = 255) String nome,
    @Size(max = 8) String ispb) {}
