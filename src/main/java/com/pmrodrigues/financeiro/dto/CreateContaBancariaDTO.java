package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new {@link com.pmrodrigues.financeiro.model.ContaBancaria}.
 */
public record CreateContaBancariaDTO(
        @NotNull Long bancoId,
        @NotNull TipoContaBancaria tipo,
        @NotBlank @Size(max = 10) String agencia,
        @NotBlank @Size(max = 20) String conta,
        @Size(max = 2) String digito,
        @Size(max = 255) String descricao,
        @Size(max = 255) String chavePix
) {
}
