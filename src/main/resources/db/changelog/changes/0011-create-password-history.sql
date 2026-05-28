-- liquibase formatted sql

-- changeset pmrodrigues:0011-create-password-history
CREATE TABLE password_history (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_history_user_created ON password_history(user_id, created_at DESC);
-- rollback DROP INDEX idx_password_history_user_created;
-- rollback DROP TABLE password_history;
