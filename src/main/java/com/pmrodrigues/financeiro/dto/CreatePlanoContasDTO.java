package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoRateio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new PlanoContas node; paiId, tipoRateio and escopoRateio are optional.
 */
public record CreatePlanoContasDTO(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank String descricao,
        @NotNull TipoConta tipo,
        Long paiId,
        TipoRateio tipoRateio,
        EscopoRateio escopoRateio
) {
}
