package com.pmrodrigues.commons.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.dto.EstadoDTO;
import com.pmrodrigues.commons.dto.EstadoFilterDTO;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.commons.service.EstadoService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstadoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean EstadoService estadoService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returnsListWith200() throws Exception {
        when(estadoService.filterBy(any(EstadoFilterDTO.class))).thenReturn(List.of(
                new EstadoDTO(1L, "São Paulo", "SP"),
                new EstadoDTO(2L, "Rio de Janeiro", "RJ")
        ));

        mockMvc.perform(get("/estados").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].uf").value("SP"));
    }

    @Test
    @WithMockUser
    void findAll_withUfFilter_callsFilterBy() throws Exception {
        when(estadoService.filterBy(any(EstadoFilterDTO.class))).thenReturn(
                List.of(new EstadoDTO(1L, "São Paulo", "SP")));

        mockMvc.perform(get("/estados").param("uf", "SP")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].uf").value("SP"));
    }

    @Test
    @WithMockUser
    void findAll_withUfFilter_notFound_returnsEmptyList() throws Exception {
        when(estadoService.filterBy(any(EstadoFilterDTO.class))).thenReturn(List.of());

        mockMvc.perform(get("/estados").param("uf", "XX")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser
    void findAll_withNomeFilter_callsFilterBy() throws Exception {
        when(estadoService.filterBy(any(EstadoFilterDTO.class))).thenReturn(
                List.of(new EstadoDTO(1L, "São Paulo", "SP")));

        mockMvc.perform(get("/estados").param("nome", "Paulo")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].nome").value("São Paulo"));
    }

    @Test
    @WithMockUser
    void findAll_includesRequestIdInResponse() throws Exception {
        when(estadoService.filterBy(any(EstadoFilterDTO.class))).thenReturn(List.of());
        var fixedId = UUID.randomUUID().toString();

        mockMvc.perform(get("/estados").header(RequestIdInterceptor.REQUEST_ID_HEADER, fixedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value(fixedId));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/estados").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(estadoService.findById(1L)).thenReturn(Optional.of(new EstadoDTO(1L, "São Paulo", "SP")));

        mockMvc.perform(get("/estados/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.uf").value("SP"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(estadoService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/estados/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/estados/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }
}
