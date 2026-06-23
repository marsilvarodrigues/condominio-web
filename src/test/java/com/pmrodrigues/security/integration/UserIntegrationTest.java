package com.pmrodrigues.security.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.CondominioRepository;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
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

class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired EstadoRepository estadoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-password";
    private static final String USER_EMAIL     = "user@test.com";
    private static final String USER_PASSWORD  = "user-password";
    private static final String TARGET_EMAIL   = "target@test.com";

    private Long targetUserId;
    private Long savedCondominioId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_condominios");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM condominios");
        jdbcTemplate.update("DELETE FROM estados");

        var estado = estadoRepository.save(Estado.builder().nome("São Paulo").uf("SP").build());
        var endereco = Endereco.builder()
                .logradouro("Rua A, 1").cep("01310100").cidade("São Paulo").estado(estado).build();
        var condominio = condominioRepository.save(
                Condominio.builder().nome("Cond Test").cnpj("11.111.111/0001-11")
                        .email("test@cond.com").endereco(endereco).build());
        savedCondominioId = condominio.getId();

        var admin = new User()
                .setEmail(ADMIN_EMAIL)
                .setPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .setName("Admin User")
                .setEnabled(true)
                .setCondominios(new HashSet<>(Set.of(condominio)))
                .setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        userRepository.save(admin);

        var regular = new User()
                .setEmail(USER_EMAIL)
                .setPassword(passwordEncoder.encode(USER_PASSWORD))
                .setName("Regular User")
                .setEnabled(true)
                .setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(regular);

        var target = new User()
                .setEmail(TARGET_EMAIL)
                .setPassword(passwordEncoder.encode("target-password"))
                .setName("Target User")
                .setEnabled(true)
                .setCondominios(new HashSet<>(Set.of(condominio)))
                .setRoles(new HashSet<>(Set.of("ROLE_USER")));
        targetUserId = userRepository.save(target).getId();
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_withAdminToken_returns200AndAllUsers() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(3));
    }

    @Test
    void findAll_withUserToken_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returns200WithUser() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(TARGET_EMAIL))
                .andExpect(jsonPath("$.data.id").value(targetUserId));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/users/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_asAdmin_returns201AndNewUser() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new CreateUserDTO("brand-new@test.com", "Brand New", Set.of("ROLE_USER"), Set.of(savedCondominioId));

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("brand-new@test.com"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_withEmptyCondominioIds_creates_globalAccessUser() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new CreateUserDTO("global@test.com", "Global Admin", Set.of("ROLE_ADMIN"), null);

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("global@test.com"));
    }

    @Test
    void create_withBlankEmail_returns400() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var body = Map.of("email", "", "name", "Brand New", "enabled", false);

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    void create_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        var dto = new CreateUserDTO("another@test.com", "Another", Set.of("ROLE_USER"), Set.of(savedCondominioId));

        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        var dto = new UserDTO(null, "another@test.com", "Another", false, null,
                Set.of(savedCondominioId), null, null);

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_asAdmin_returns200WithUpdatedUser() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new UserDTO(null, "updated@test.com", "Updated Name", true,
                Set.of("ROLE_USER"), Set.of(savedCondominioId), null, null);

        mockMvc.perform(put("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("updated@test.com"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    void update_withBlankEmail_returns400() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var body = Map.of("email", "", "name", "Updated Name", "enabled", false);

        mockMvc.perform(put("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    void update_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        var dto = new UserDTO(null, "x@test.com", "X", true, null, Set.of(savedCondominioId), null, null);

        mockMvc.perform(put("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutToken_returns401() throws Exception {
        var dto = new UserDTO(null, "x@test.com", "X", true, null, Set.of(savedCondominioId), null, null);

        mockMvc.perform(put("/users/" + targetUserId)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_asAdmin_returns204() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(delete("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/users/" + targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/users/" + targetUserId)
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
