package com.pmrodrigues.financeiro.dto;

import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoRateio;

import java.util.List;

/**
 * Tree representation of a PlanoContas node including its children for the /arvore endpoint.
 */
public record PlanoContasNodeDTO(
        Long id,
        String codigo,
        String descricao,
        TipoConta tipo,
        TipoRateio tipoRateio,
        EscopoRateio escopoRateio,
        Long paiId,
        List<PlanoContasNodeDTO> filhos
) {
}
