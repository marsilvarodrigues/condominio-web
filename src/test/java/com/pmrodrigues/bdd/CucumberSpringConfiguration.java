package com.pmrodrigues.bdd;

import com.pmrodrigues.commons.service.MailService;
import com.pmrodrigues.gateway.service.AsaasGatewayService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Configuração Spring para o contexto Cucumber.
 *
 * <p>Esta classe cumpre três papéis ao mesmo tempo:
 * <ul>
 *   <li>{@code @CucumberContextConfiguration}: registra o contexto Spring compartilhado
 *       por todos os cenários (criado uma vez por suite)</li>
 *   <li>{@code @SpringBootTest(RANDOM_PORT)}: inicializa a aplicação completa em uma porta
 *       aleatória — os steps usam RestTemplate contra o servidor real, não MockMvc</li>
 *   <li>Testcontainers: inicia PostgreSQL e Redis em containers Docker antes do
 *       contexto Spring ser criado (bloco {@code static})</li>
 * </ul>
 *
 * <p><b>Por que bloco static em vez de @Container @Testcontainers?</b><br>
 * A extensão JUnit 5 {@code @Testcontainers} não dispara no Cucumber porque os cenários
 * não são métodos {@code @Test} normais. O bloco estático garante que os containers
 * estejam rodando antes de {@link #overrideProperties} ser chamado durante a
 * inicialização do ApplicationContext.
 *
 * <p><b>MailService mockado</b>: {@code UserService.create()} envia e-mail de ativação.
 * O mock evita erros de conexão SMTP em ambiente de teste.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {

    // ── Containers iniciados uma única vez para toda a suite BDD ──────────────
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("security.jwt.issuer", () -> "http://localhost:8080");
        registry.add("security.jwt.access-token-expiration", () -> "3600");
        registry.add("security.jwt.refresh-token-expiration", () -> "86400");
        registry.add("security.jwt.client-id", () -> "condominio");
        registry.add("security.jwt.client-secret", () -> "condominio-secret");
        registry.add("app.security.rate-limit.login.max-requests", () -> "1000");
        registry.add("app.security.rate-limit.refresh.max-requests", () -> "1000");
    }

    @MockitoBean
    MailService mailService;

    @MockitoBean
    AsaasGatewayService asaasGatewayService;
}
