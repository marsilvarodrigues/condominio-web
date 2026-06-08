package com.pmrodrigues.condominio.dto;

import com.pmrodrigues.commons.validation.Cnpj;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for creating a new condominium.
 *
 * <p>All fields are mandatory. {@code cnpj} is validated both for format and check digits using the
 * official Brazilian algorithm. {@code endereco.cep} must be exactly 8 digits and {@code
 * endereco.estado} must be the primary key of an existing state record.
 */
public record CreateCondominioDTO(
    @NotBlank String nome,
    @NotBlank @Cnpj String cnpj,
    @NotBlank @Email String email,
    @NotNull @Valid CreateCondominioEnderecoDTO endereco) {}
