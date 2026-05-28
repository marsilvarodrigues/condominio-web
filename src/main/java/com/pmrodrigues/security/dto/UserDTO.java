package com.pmrodrigues.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO de usuário para operações via API REST.
 *
 * <p><b>Regra de negócio</b>: {@code condominioId} é obrigatório em todas as operações
 * de criação e atualização via API. O usuário "master" (sem condomínio associado) só
 * pode ser criado diretamente via SQL — a validação {@code @NotNull} neste campo
 * garante que a API não permita criar usuários sem vínculo a um condomínio.
 *
 * <p>Para criar o usuário master em produção, execute via SQL:
 * <pre>
 *   INSERT INTO users (email, password, name, enabled, deleted)
 *   VALUES ('master@exemplo.com', '{bcrypt}[hash]', 'Master Admin', true, false);
 *   INSERT INTO user_roles (user_id, role)
 *   SELECT id, 'ROLE_ADMIN' FROM users WHERE email = 'master@exemplo.com';
 * </pre>
 * Gere o hash BCrypt com: {@code new BCryptPasswordEncoder().encode("senha-forte")}
 */
public record UserDTO(
        Long id,
        @Email
        @NotBlank
        String email,
        @NotBlank
        @NotNull
        String name,
        boolean enabled,
        Set<String> roles,
        @NotNull
        Long condominioId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
