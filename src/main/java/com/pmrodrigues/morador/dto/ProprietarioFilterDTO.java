package com.pmrodrigues.morador.dto;

/**
 * Filter criteria for querying {@code Proprietario} entities. All fields are optional.
 */
public record ProprietarioFilterDTO(Long apartamentoId, Long pessoaId, Boolean apenasAtivos) {}
