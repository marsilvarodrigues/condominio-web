package com.pmrodrigues.condominio.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.BlocoRepository;
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

class BlocoIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EstadoRepository estadoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-pass";
    private static final String USER_EMAIL     = "user@test.com";
    private static final String USER_PASSWORD  = "user-pass";

    private Condominio savedCondominio;
    private Long blocoAId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM blocos");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM condominios");
        jdbcTemplate.update("DELETE FROM estados");

        var estado = estadoRepository.save(Estado.builder().nome("São Paulo").uf("SP").build());
        var endereco = Endereco.builder()
                .logradouro("Rua A, 1").cep("01310100").cidade("São Paulo").estado(estado).build();
        savedCondominio = condominioRepository.save(
                Condominio.builder().nome("Cond X").cnpj("11.111.111/0001-11")
                        .email("x@test.com").endereco(endereco).build());

        blocoAId = blocoRepository.save(
                Bloco.builder().condominio(savedCondominio).numero(1).bloco("A").build()).getId();
        blocoRepository.save(
                Bloco.builder().condominio(savedCondominio).numero(2).bloco("B").build());

        var admin = new User().setEmail(ADMIN_EMAIL).setPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .setName("Admin").setEnabled(true).setCondominioId(savedCondominio.getId())
                .setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        userRepository.save(admin);

        var user = new User().setEmail(USER_EMAIL).setPassword(passwordEncoder.encode(USER_PASSWORD))
                .setName("User").setEnabled(true).setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(user);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_withAdminToken_returnsBlocos() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/blocos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void findAll_withBlocoFilter_returnsFiltered() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/blocos").param("bloco", "A")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].bloco").value("A"));
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/blocos/" + blocoAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(blocoAId))
                .andExpect(jsonPath("$.data.bloco").value("A"));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/blocos/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_asAdmin_returns201AndNewBloco() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new CreateBlocoDTO(3, "C");

        mockMvc.perform(post("/blocos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bloco").value("C"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(post("/blocos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlocoDTO(3, "C"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlocoDTO(3, "C"))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_asAdmin_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new BlocoDTO(null, 10, "A-Updated", null, null);

        mockMvc.perform(put("/blocos/" + blocoAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bloco").value("A-Updated"));
    }

    @Test
    void update_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        var dto = new BlocoDTO(null, 10, "A-Updated", null, null);

        mockMvc.perform(put("/blocos/" + blocoAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_asAdmin_returns204() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(delete("/blocos/" + blocoAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/blocos/" + blocoAId)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/blocos/" + blocoAId)
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
