package com.pmrodrigues.morador.dto;

/**
 * Filter criteria for querying {@code Pessoa} entities. All fields are optional; absent (null)
 * fields are ignored by the specification.
 *
 * @param tipo discriminator value: MORADOR | PROP_PF | PROP_PJ
 */
public record PessoaFilterDTO(String nome, String tipo, String cpf, String email) {}
