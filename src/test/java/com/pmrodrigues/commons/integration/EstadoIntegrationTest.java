package com.pmrodrigues.commons.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
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

class EstadoIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EstadoRepository estadoRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-pass";

    private Long spId;
    private Long rjId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM estados");

        spId = estadoRepository.save(Estado.builder().nome("São Paulo").uf("SP").build()).getId();
        rjId = estadoRepository.save(Estado.builder().nome("Rio de Janeiro").uf("RJ").build()).getId();
        estadoRepository.save(Estado.builder().nome("Minas Gerais").uf("MG").build());

        var admin = new User()
                .setEmail(ADMIN_EMAIL)
                .setPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .setName("Admin")
                .setEnabled(true)
                .setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        userRepository.save(admin);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_withAdminToken_returnsAllEstados() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/estados")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void findAll_withUfFilter_returnsSingleEstado() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/estados").param("uf", "SP")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].uf").value("SP"));
    }

    @Test
    void findAll_withNomeFilter_returnsMatchingEstados() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/estados").param("nome", "Paulo")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nome").value("São Paulo"));
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/estados")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/estados/" + spId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uf").value("SP"))
                .andExpect(jsonPath("$.data.id").value(spId));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/estados/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
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
