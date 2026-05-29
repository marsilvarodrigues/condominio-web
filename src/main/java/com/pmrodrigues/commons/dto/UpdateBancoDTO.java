package com.pmrodrigues.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for updating an existing {@link com.pmrodrigues.commons.model.Banco}.
 */
public record UpdateBancoDTO(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 8) String ispb
) {
}
