package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.CreatePlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasDTO;
import com.pmrodrigues.financeiro.dto.PlanoContasFilterDTO;
import com.pmrodrigues.financeiro.model.EscopoRateio;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.model.TipoRateio;
import com.pmrodrigues.financeiro.service.PlanoContasService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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
class PlanoContasControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PlanoContasService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private PlanoContasDTO dto(Long id) {
        return new PlanoContasDTO(id, "1", "Receitas", TipoConta.RECEITA, null, null, null, null, null);
    }

    private PlanoContasDTO dtoWithRateio(Long id) {
        return new PlanoContasDTO(id, "2", "Taxas", TipoConta.RECEITA,
                TipoRateio.IGUALITARIO, EscopoRateio.TODOS, null, null, null);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAll_returns200WithList() throws Exception {
        when(service.filterBy(any(PlanoContasFilterDTO.class))).thenReturn(List.of(dto(1L), dto(2L)));

        mockMvc.perform(get("/plano-contas").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/plano-contas").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_whenFound_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(dto(1L)));

        mockMvc.perform(get("/plano-contas/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value("1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_whenFound_includesTipoRateioAndEscopo() throws Exception {
        when(service.findById(2L)).thenReturn(Optional.of(dtoWithRateio(2L)));

        mockMvc.perform(get("/plano-contas/2").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipoRateio").value("IGUALITARIO"))
                .andExpect(jsonPath("$.data.escopoRateio").value("TODOS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_whenNotFound_returns404() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/plano-contas/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(service.create(any())).thenReturn(dto(1L));

        mockMvc.perform(post("/plano-contas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePlanoContasDTO("1", "Receitas", TipoConta.RECEITA, null, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codigo").value("1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withRateioAndEscopo_returns201() throws Exception {
        when(service.create(any())).thenReturn(dtoWithRateio(2L));

        mockMvc.perform(post("/plano-contas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePlanoContasDTO("2", "Taxas", TipoConta.RECEITA,
                                        null, TipoRateio.IGUALITARIO, EscopoRateio.TODOS))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipoRateio").value("IGUALITARIO"))
                .andExpect(jsonPath("$.data.escopoRateio").value("TODOS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankCodigo_returns400() throws Exception {
        mockMvc.perform(post("/plano-contas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"\",\"descricao\":\"x\",\"tipo\":\"RECEITA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/plano-contas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePlanoContasDTO("1", "x", TipoConta.RECEITA, null, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/plano-contas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePlanoContasDTO("1", "x", TipoConta.RECEITA, null, null, null))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(service.update(any())).thenReturn(dto(1L));

        mockMvc.perform(put("/plano-contas/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/plano-contas/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L))))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/plano-contas/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/plano-contas/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }
}
