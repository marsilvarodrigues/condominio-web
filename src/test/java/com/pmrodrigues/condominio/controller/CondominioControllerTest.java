package com.pmrodrigues.condominio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.condominio.dto.CondominioDTO;
import com.pmrodrigues.condominio.dto.CondominioFilterDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioDTO;
import com.pmrodrigues.condominio.dto.CreateCondominioEnderecoDTO;
import com.pmrodrigues.condominio.service.CondominioService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CondominioControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CondominioService condominioService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returnsListWith200() throws Exception {
        when(condominioService.filterBy(any(CondominioFilterDTO.class))).thenReturn(List.of(
                dto(1L, "Cond A", "11.111.111/0001-11"),
                dto(2L, "Cond B", "22.222.222/0001-22")
        ));

        mockMvc.perform(get("/condominios").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void findAll_withNomeFilter_delegatesToFilterBy() throws Exception {
        when(condominioService.filterBy(any(CondominioFilterDTO.class)))
                .thenReturn(List.of(dto(1L, "Cond A", "11.111.111/0001-11")));

        mockMvc.perform(get("/condominios").param("nome", "Cond A")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nome").value("Cond A"));
    }

    @Test
    @WithMockUser
    void findAll_withCnpjFilter_delegatesToFilterBy() throws Exception {
        when(condominioService.filterBy(any(CondominioFilterDTO.class)))
                .thenReturn(List.of(dto(1L, "Cond A", "11.111.111/0001-11")));

        mockMvc.perform(get("/condominios").param("cnpj", "11.111.111/0001-11")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].cnpj").value("11.111.111/0001-11"));
    }

    @Test
    @WithMockUser
    void findAll_withBothFilters_delegatesToFilterBy() throws Exception {
        when(condominioService.filterBy(any(CondominioFilterDTO.class)))
                .thenReturn(List.of(dto(1L, "Cond A", "11.111.111/0001-11")));

        mockMvc.perform(get("/condominios")
                        .param("nome", "Cond A").param("cnpj", "11.111.111/0001-11")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/condominios").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(condominioService.findById(1L)).thenReturn(Optional.of(dto(1L, "Cond A", "11.111.111/0001-11")));

        mockMvc.perform(get("/condominios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Cond A"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(condominioService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/condominios/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/condominios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(condominioService.create(any())).thenReturn(dto(1L, "Cond A", "12.345.678/0001-95"));

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("Cond A", "12.345.678/0001-95"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Cond A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankNome_returns400() throws Exception {
        var body = Map.of("nome", "", "cnpj", "12.345.678/0001-95", "email", "a@test.com");

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.nome").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankCnpj_returns400() throws Exception {
        var body = Map.of("nome", "Cond A", "cnpj", "", "email", "a@test.com");

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.cnpj").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidCnpj_returns400() throws Exception {
        var body = Map.of("nome", "Cond A", "cnpj", "11.111.111/0001-11", "email", "a@test.com");

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.cnpj").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidEmail_returns400() throws Exception {
        var body = Map.of("nome", "Cond A", "cnpj", "12.345.678/0001-95", "email", "not-an-email");

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidCep_returns400() throws Exception {
        var endereco = Map.of("logradouro", "Rua A", "cep", "0100", "cidade", "SP", "estado", 1);
        var body = Map.of("nome", "Cond A", "cnpj", "12.345.678/0001-95", "email", "a@test.com", "endereco", endereco);

        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields['endereco.cep']").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("Cond A", "12.345.678/0001-95"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/condominios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("Cond A", "12.345.678/0001-95"))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(condominioService.update(any())).thenReturn(dto(1L, "Cond Updated", "11.111.111/0001-11"));

        mockMvc.perform(put("/condominios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "Cond Updated", "11.111.111/0001-11"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Cond Updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankNome_returns400() throws Exception {
        var body = Map.of("nome", "", "cnpj", "11.111.111/0001-11", "email", "a@test.com");

        mockMvc.perform(put("/condominios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.nome").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/condominios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "Cond A", "11.111.111/0001-11"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/condominios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "Cond A", "11.111.111/0001-11"))))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(condominioService).delete(1L);

        mockMvc.perform(delete("/condominios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/condominios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/condominios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private CondominioDTO dto(Long id, String nome, String cnpj) {
        return new CondominioDTO(id, nome, cnpj, "test@test.com", null, null, null);
    }

    private CreateCondominioDTO createDto(String nome, String cnpj) {
        return new CreateCondominioDTO(nome, cnpj, "test@test.com",
                new CreateCondominioEnderecoDTO("Rua A, 1", "01001000", "São Paulo", 1L));
    }
}
