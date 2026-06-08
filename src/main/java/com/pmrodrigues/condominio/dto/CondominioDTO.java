package com.pmrodrigues.condominio.dto;

import com.pmrodrigues.commons.dto.EnderecoDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Represents a full condominium complex for read, create, and update operations, including audit
 * timestamps.
 */
public record CondominioDTO(
    Long id,
    @NotBlank String nome,
    @NotBlank String cnpj,
    @NotBlank @Email String email,
    EnderecoDTO endereco,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
