package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoRateio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** Full representation of a PlanoContas node, used for updates and responses. */
public record PlanoContasDTO(
    Long id,
    @NotBlank @Size(max = 20) String codigo,
    @NotBlank String descricao,
    @NotNull TipoConta tipo,
    TipoRateio tipoRateio,
    EscopoRateio escopoRateio,
    Long paiId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
