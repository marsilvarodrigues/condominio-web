-- liquibase formatted sql

-- changeset pmrodrigues:0001-create-users-table
CREATE TABLE users (
    id       BIGSERIAL    PRIMARY KEY,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name     VARCHAR(255) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_users_email UNIQUE (email)
);
-- rollback DROP TABLE users;

-- changeset pmrodrigues:0001-create-user-roles-table
CREATE TABLE user_roles (
    user_id BIGINT       NOT NULL,
    role    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- rollback DROP TABLE user_roles;