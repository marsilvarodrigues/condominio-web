package com.pmrodrigues.condominio.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.embeddable.Endereco;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.model.Estado;
import com.pmrodrigues.commons.repository.EstadoRepository;
import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.model.Apartamento;
import com.pmrodrigues.condominio.model.Bloco;
import com.pmrodrigues.condominio.model.Condominio;
import com.pmrodrigues.condominio.repository.ApartamentoRepository;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApartamentoIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EstadoRepository estadoRepository;
    @Autowired CondominioRepository condominioRepository;
    @Autowired BlocoRepository blocoRepository;
    @Autowired ApartamentoRepository apartamentoRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL    = "admin@test.com";
    private static final String ADMIN_PASSWORD = "admin-pass";
    private static final String USER_EMAIL     = "user@test.com";
    private static final String USER_PASSWORD  = "user-pass";

    private Condominio savedCondominio;
    private Bloco savedBloco;
    private Long apt101Id;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM apartamentos");
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
        savedBloco = blocoRepository.save(
                Bloco.builder().condominio(savedCondominio).numero(1).bloco("A").build());

        apt101Id = apartamentoRepository.save(
                Apartamento.builder().condominio(savedCondominio).bloco(savedBloco).numero("101").areaConstruida(BigDecimal.TEN).build()).getId();
        apartamentoRepository.save(
                Apartamento.builder().condominio(savedCondominio).bloco(savedBloco).numero("102").areaConstruida(BigDecimal.TEN).build());

        var admin = new User().setEmail(ADMIN_EMAIL).setPassword(passwordEncoder.encode(ADMIN_PASSWORD))
                .setName("Admin").setEnabled(true).setCondominios(new HashSet<>(Set.of(savedCondominio)))
                .setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        userRepository.save(admin);

        var user = new User().setEmail(USER_EMAIL).setPassword(passwordEncoder.encode(USER_PASSWORD))
                .setName("User").setEnabled(true).setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(user);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_withAdminToken_returnsApartamentos() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/apartamentos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void findAll_withBlocoIdFilter_returnsFiltered() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/apartamentos").param("blocoId", savedBloco.getId().toString())
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void findAll_withNumeroFilter_returnsFiltered() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/apartamentos").param("numero", "101")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].numero").value("101"));
    }

    @Test
    void findAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_whenFound_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/apartamentos/" + apt101Id)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(apt101Id))
                .andExpect(jsonPath("$.data.numero").value("101"));
    }

    @Test
    void findById_whenNotFound_returns404() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/apartamentos/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    void create_asAdmin_returns201AndNewApartamento() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new CreateApartamentoDTO(savedBloco.getId(), "201", BigDecimal.TEN, null, null);

        mockMvc.perform(post("/apartamentos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value("201"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void create_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(post("/apartamentos")
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApartamentoDTO(savedBloco.getId(), "201", BigDecimal.TEN, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApartamentoDTO(savedBloco.getId(), "201", BigDecimal.TEN, null, null))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_asAdmin_returns200() throws Exception {
        var token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        var dto = new ApartamentoDTO(null, null, null, "101-A", null, null, BigDecimal.TEN, null, null, 0);

        mockMvc.perform(put("/apartamentos/" + apt101Id)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numero").value("101-A"));
    }

    @Test
    void update_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
        var dto = new ApartamentoDTO(null, null, null, "101-A", null, null, BigDecimal.TEN, null, null, 0);

        mockMvc.perform(put("/apartamentos/" + apt101Id)
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

        mockMvc.perform(delete("/apartamentos/" + apt101Id)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asUser_returns403() throws Exception {
        var token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/apartamentos/" + apt101Id)
                        .header("Authorization", "Bearer " + token)
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/apartamentos/" + apt101Id)
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
