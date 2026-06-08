package com.pmrodrigues.condominio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CNPJ;

/**
 * Payload for creating a new condominium.
 *
 * <p>All fields are mandatory. {@code cnpj} is validated both for format and check digits using the
 * official Brazilian algorithm. {@code endereco.cep} must be exactly 8 digits and {@code
 * endereco.estado} must be the primary key of an existing state record.
 */
public record CreateCondominioDTO(
    @NotBlank String nome,
    @NotBlank @CNPJ String cnpj,
    @NotBlank @Email String email,
    @NotNull @Valid CreateCondominioEnderecoDTO endereco) {}
