package com.pmrodrigues.morador.dto;

/**
 * Partial-update payload for an existing {@link com.pmrodrigues.morador.model.Morador}.
 * Null fields are ignored by the mapper (NullValuePropertyMappingStrategy.IGNORE).
 */
public record UpdatePessoaDTO(
        String nome,
        String email,
        String telefone,
        String cpf
) {}
