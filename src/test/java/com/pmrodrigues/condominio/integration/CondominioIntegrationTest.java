package com.pmrodrigues.condominio.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.UserRepository;
import com.pmrodrigues.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CondominioIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EstadoRepository estadoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-pass";
    private static final String USER_EMAIL     = "user@test.com";
    private static final String USER_PASSWORD  = "user-pass";

    private Estado savedEstado;
    private Long condAId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM condominios");
        jdbcTemplate.update("DELETE FROM estados");

        savedEstado = estadoRepository.save(Estado.builder().nome("São Paulo").uf("SP").build());

        var endereco = Endereco.builder()
                .logradouro("Rua A, 1").cep("01310100").cidade("São Paulo").estado(savedEstado).build();
        condAId = condominioRepository.save(
                Condominio.builder().nome("Cond A").cnpj("11.111.111/0001-11")
                        .email("a@test.com").endereco(endereco).build()).getId();

        var endereco2 = Endereco.builder()
                .logradouro("Rua B, 2").cep("01310200").cidade("São Paulo").estado(savedEstado).build();
        condominioRepository.save(
                Condominio.builder().nome("Cond B").cnpj("22.222.222/0001-22")
                        .email("b@test.com").endereco(endereco2).build());

        var admin = new User().setEmail(ADMIN_EMAIL).setPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .setName("Admin").setEnabled(true).setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        userRepository.save(admin);

        var user = new User().setEmail(USER_EMAIL).setPassword(passwordEncoder.encode(USER_PASSWORD))
                .setName("User").setEnabled(true).setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(user);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_withAdminToken_returnsAllCondominios() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/condominios")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void findAll_withNomeFilter_returnsFiltered() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/condominios").param("nome", "Cond A")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nome").value("Cond A"));
    }

    @Test
    void findAll_withCnpjFilter_returnsFiltered() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/condominios").param("cnpj", "11.111.111/0001-11")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].cnpj").value("11.111.111/0001-11"));
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/condominios/" + condAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(condAId))
                .andExpect(jsonPath("$.data.nome").value("Cond A"));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/condominios/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_asAdmin_returns201AndNewCondominio() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var endereco = new CreateCondominioEnderecoDTO("Rua Nova, 10", "01310300", "São Paulo", savedEstado.getId());
        var dto = new CreateCondominioDTO("Cond New", "12.345.678/0001-95", "new@test.com", endereco);

        mockMvc.perform(post("/condominios")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nome").value("Cond New"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_withBlankNome_returns400() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var body = Map.of("nome", "", "cnpj", "44.444.444/0001-44", "email", "d@test.com");

        mockMvc.perform(post("/condominios")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"));
    }

    @Test
    void create_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        var endereco = new CreateCondominioEnderecoDTO("Rua X, 1", "01001000", "São Paulo", savedEstado.getId());
        var dto = new CreateCondominioDTO("Cond X", "98.765.432/0001-98", "x@test.com", endereco);

        mockMvc.perform(post("/condominios")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        var dto = Map.of("nome", "X", "cnpj", "55.555.555/0001-55", "email", "x@test.com");

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_asAdmin_returns204() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(delete("/condominios/" + condAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/condominios/" + condAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/condominios/" + condAId)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── helper ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String loginAndGetToken(String email, String password) throws Exception {
        var body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (String) objectMapper.readValue(body, Map.class).get("accessToken");
    }
}
