package com.pmrodrigues.support;

import com.pmrodrigues.commons.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests requiring a real PostgreSQL database and Redis cache.
 *
 * <p>The containers are started once for the entire test run (shared static fields). Every
 * subclass that calls {@link #configureProperties} registers the same JDBC URL and Redis
 * coordinates, so Spring Test's {@code ApplicationContext} cache finds a match for every
 * subclass — one context is created and reused by all six integration-test classes instead
 * of one context per class.
 *
 * <p>{@link MailService} is mocked here to prevent the real SMTP bean from attempting a
 * connection during context startup (which would fail without an SMTP server).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @MockitoBean
    MailService mailService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Delete leaf-to-root in FK order so subclass @BeforeEach always starts clean,
        // regardless of which other test class ran previously on the shared database.
        jdbcTemplate.update("DELETE FROM cotas_rateio");
        jdbcTemplate.update("DELETE FROM rateio_execucoes");
        jdbcTemplate.update("DELETE FROM despesas");
        jdbcTemplate.update("DELETE FROM coeficientes_rateio");
        jdbcTemplate.update("DELETE FROM grupos_despesa");
        jdbcTemplate.update("DELETE FROM apartamentos");
        jdbcTemplate.update("DELETE FROM blocos");
        jdbcTemplate.update("DELETE FROM user_condominios");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM condominios");
        jdbcTemplate.update("DELETE FROM estados");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",               POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",          POSTGRES::getUsername);
        registry.add("spring.datasource.password",          POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                     () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("security.jwt.issuer",                   () -> "http://localhost:8080");
        registry.add("security.jwt.access-token-expiration",  () -> "3600");
        registry.add("security.jwt.refresh-token-expiration", () -> "86400");
        registry.add("security.jwt.client-id",                () -> "condominio");
        registry.add("security.jwt.client-secret",            () -> "condominio-secret");
        registry.add("app.security.rate-limit.login.max-requests",   () -> "1000");
        registry.add("app.security.rate-limit.refresh.max-requests", () -> "1000");
    }
}
