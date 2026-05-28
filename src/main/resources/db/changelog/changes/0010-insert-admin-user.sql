-- liquibase formatted sql

-- changeset pmrodrigues:0010-insert-admin-user
-- Cria o usuário administrador global (sem condominio_id) para gerenciar o sistema.
-- Senha padrão: Admin@123 — ALTERE via API antes de colocar em produção.
-- Hash BCrypt (cost 10) gerado com: new BCryptPasswordEncoder().encode("Admin@123")
INSERT INTO users (email, password, name, enabled, deleted, created_at, updated_at)
VALUES (
    'admin@condominio.com',
    '$2b$10$A3CuuCrsN.FDneitoMDtdOwEnjHMspsFHoN0KVH/CX.QpO.3FLe6S',
    'Administrador',
    true,
    false,
    NOW(),
    NOW()
);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE email = 'admin@condominio.com';

-- rollback DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'admin@condominio.com');
-- rollback DELETE FROM users WHERE email = 'admin@condominio.com';