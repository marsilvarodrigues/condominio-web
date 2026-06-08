package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de usuário para operações via API REST.
 *
 * <p>{@code condominioIds} é opcional. Usuários sem nenhum condomínio associado têm acesso global a
 * todos os dados (perfil master). Usuários com um ou mais condominios têm acesso restrito aos
 * respectivos dados.
 */
public record UserDTO(
    Long id,
    @Email @NotBlank String email,
    @NotBlank @NotNull String name,
    boolean enabled,
    Set<String> roles,
    Set<Long> condominioIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
