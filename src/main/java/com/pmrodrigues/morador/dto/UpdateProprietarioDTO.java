package com.pmrodrigues.morador.dto;

/**
 * Partial-update payload for an existing {@code Proprietario}. The {@code tipo} discriminator
 * cannot be changed after creation. Null fields are ignored by the mapper
 * (NullValuePropertyMappingStrategy.IGNORE).
 */
public record UpdateProprietarioDTO(
    String nome, String email, String telefone, String cpf, String cnpj, String razaoSocial) {}
