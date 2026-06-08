package com.pmrodrigues.morador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Creation payload for a {@link com.pmrodrigues.morador.model.Morador}.
 *
 * @param nome full name
 * @param email login e-mail address
 * @param telefone optional phone number
 * @param cpf Brazilian individual tax identifier (required)
 * @param apartamentoId optional apartment to assign the morador to
 */
public record CreatePessoaDTO(
    @NotBlank String nome,
    @NotBlank @Email String email,
    String telefone,
    @NotBlank String cpf,
    Long apartamentoId) {}
