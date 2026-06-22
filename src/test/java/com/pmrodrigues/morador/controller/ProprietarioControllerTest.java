package com.pmrodrigues.morador.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.morador.dto.CreateProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioDTO;
import com.pmrodrigues.morador.dto.ProprietarioFilterDTO;
import com.pmrodrigues.morador.dto.UpdateProprietarioDTO;
import com.pmrodrigues.morador.service.ProprietarioService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProprietarioControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ProprietarioService proprietarioService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private ProprietarioDTO proprietarioDTO() {
        return new ProprietarioDTO(1L, "João Silva", "PROP_PF", "111.111.111-11",
                null, null, "joao@test.com", null, List.of(), 1L, null, null);
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void filterBy_returns200() throws Exception {
        Page<ProprietarioDTO> page = new PageImpl<>(List.of(proprietarioDTO()));
        when(proprietarioService.filterBy(any(ProprietarioFilterDTO.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/proprietarios").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_returns200() throws Exception {
        when(proprietarioService.findById(1L)).thenReturn(proprietarioDTO());

        mockMvc.perform(get("/proprietarios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("João Silva"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_notFound_returns404() throws Exception {
        when(proprietarioService.findById(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Not found"));

        mockMvc.perform(get("/proprietarios/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        var body = new CreateProprietarioDTO("João", "joao@test.com", null, "PROP_PF", "111.111.111-11", null, null);
        when(proprietarioService.create(any(CreateProprietarioDTO.class))).thenReturn(proprietarioDTO());

        mockMvc.perform(post("/proprietarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        var body = new CreateProprietarioDTO("João", "joao@test.com", null, "PROP_PF", "111.111.111-11", null, null);

        mockMvc.perform(post("/proprietarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void create_asUser_returns403() throws Exception {
        var body = new CreateProprietarioDTO("João", "joao@test.com", null, "PROP_PF", "111.111.111-11", null, null);

        mockMvc.perform(post("/proprietarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        var body = new UpdateProprietarioDTO("Novo Nome", null, null, null, null, null);
        when(proprietarioService.update(anyLong(), any(UpdateProprietarioDTO.class))).thenReturn(proprietarioDTO());

        mockMvc.perform(put("/proprietarios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_returns404() throws Exception {
        when(proprietarioService.update(anyLong(), any(UpdateProprietarioDTO.class)))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Not found"));

        mockMvc.perform(put("/proprietarios/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProprietarioDTO(null, null, null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(proprietarioService).delete(1L);

        mockMvc.perform(delete("/proprietarios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    // ── associarApartamento ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void associarApartamento_asAdmin_returns200() throws Exception {
        when(proprietarioService.associarApartamento(1L, 5L)).thenReturn(proprietarioDTO());

        mockMvc.perform(post("/proprietarios/1/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .param("apartamentoId", "5"))
                .andExpect(status().isOk());
    }

    // ── desassociarApartamento ────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void desassociarApartamento_asAdmin_returns200() throws Exception {
        when(proprietarioService.desassociarApartamento(1L, 5L)).thenReturn(proprietarioDTO());

        mockMvc.perform(delete("/proprietarios/1/apartamentos/5")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk());
    }

    // ── findByApartamento ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findByApartamento_returns200() throws Exception {
        when(proprietarioService.findByApartamento(1L)).thenReturn(List.of(proprietarioDTO()));

        mockMvc.perform(get("/apartamentos/1/proprietarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── meusImoveis ───────────────────────────────────────────────────────

    @Test
    void meusImoveis_asProprietario_returns200() throws Exception {
        when(proprietarioService.findById(42L)).thenReturn(proprietarioDTO());

        mockMvc.perform(get("/proprietarios/meus-imoveis")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .with(jwt()
                                .jwt(j -> j.claim("user_id", 42))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROPRIETARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("João Silva"));
    }

    @Test
    void meusImoveis_claimMissing_returns404() throws Exception {
        mockMvc.perform(get("/proprietarios/meus-imoveis")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROPRIETARIO"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void meusImoveis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/proprietarios/meus-imoveis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void meusImoveis_asUser_returns403() throws Exception {
        mockMvc.perform(get("/proprietarios/meus-imoveis")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }
}
