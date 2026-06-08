package com.pmrodrigues.morador.dto;

import java.time.LocalDateTime;

/**
 * Read DTO for a {@code Pessoa} entity. The {@code tipo} field carries the SINGLE_TABLE
 * discriminator value: {@code MORADOR}, {@code PROP_PF}, or {@code PROP_PJ}.
 */
public record PessoaDTO(
    Long id,
    String nome,
    /** Discriminator: MORADOR | PROP_PF | PROP_PJ */
    String tipo,
    String cpf,
    String email,
    String telefone,
    Long apartamentoId,
    String apartamentoNumero,
    Long userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
