package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.model.OrigemLancamento;
import com.pmrodrigues.financeiro.model.StatusLancamento;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import com.pmrodrigues.financeiro.service.LancamentoBancarioService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LancamentoBancarioControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean LancamentoBancarioService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private LancamentoBancarioDTO lancamentoDto(Long id) {
        return new LancamentoBancarioDTO(id, 1L, "Conta Principal",
                LocalDate.now(), new BigDecimal("500.00"),
                TipoLancamento.CREDITO, "Pagamento taxa",
                OrigemLancamento.MANUAL, null,
                StatusLancamento.PENDENTE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returns200WithPage() throws Exception {
        var page = new PageImpl<>(List.of(lancamentoDto(1L)), PageRequest.of(0, 10), 1);
        when(service.filterBy(any(), any())).thenReturn(page);

        mockMvc.perform(get("/lancamentos-bancarios").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/lancamentos-bancarios").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(lancamentoDto(1L));

        mockMvc.perform(get("/lancamentos-bancarios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo").value("CREDITO"))
                .andExpect(jsonPath("$.data.status").value("PENDENTE"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "LancamentoBancario not found: 99"));

        mockMvc.perform(get("/lancamentos-bancarios/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(service.create(any())).thenReturn(lancamentoDto(10L));

        var dto = new CreateLancamentoBancarioDTO(1L, LocalDate.now(),
                new BigDecimal("500.00"), TipoLancamento.CREDITO,
                "Pagamento taxa", OrigemLancamento.MANUAL, null);
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withNullContaBancariaId_returns400() throws Exception {
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataLancamento\":\"2026-01-01\",\"valor\":100,\"tipo\":\"CREDITO\",\"descricao\":\"Teste\",\"origem\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withNegativeValor_returns400() throws Exception {
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaBancariaId\":1,\"dataLancamento\":\"2026-01-01\",\"valor\":-10,\"tipo\":\"CREDITO\",\"descricao\":\"Teste\",\"origem\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankDescricao_returns400() throws Exception {
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contaBancariaId\":1,\"dataLancamento\":\"2026-01-01\",\"valor\":100,\"tipo\":\"CREDITO\",\"descricao\":\"\",\"origem\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        var dto = new CreateLancamentoBancarioDTO(1L, LocalDate.now(),
                new BigDecimal("500.00"), TipoLancamento.CREDITO,
                "Pagamento", OrigemLancamento.MANUAL, null);
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        var dto = new CreateLancamentoBancarioDTO(1L, LocalDate.now(),
                new BigDecimal("500.00"), TipoLancamento.CREDITO,
                "Pagamento", OrigemLancamento.MANUAL, null);
        mockMvc.perform(post("/lancamentos-bancarios")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(lancamentoDto(1L));

        var dto = new UpdateLancamentoBancarioDTO(LocalDate.now(), new BigDecimal("750.00"), "Atualizado", null);
        mockMvc.perform(put("/lancamentos-bancarios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withNullDataLancamento_returns400() throws Exception {
        mockMvc.perform(put("/lancamentos-bancarios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100,\"descricao\":\"Teste\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_whenNotFound_returns404() throws Exception {
        when(service.update(eq(99L), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "LancamentoBancario not found: 99"));

        var dto = new UpdateLancamentoBancarioDTO(LocalDate.now(), BigDecimal.TEN, "Teste", null);
        mockMvc.perform(put("/lancamentos-bancarios/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        var dto = new UpdateLancamentoBancarioDTO(LocalDate.now(), BigDecimal.TEN, "Teste", null);
        mockMvc.perform(put("/lancamentos-bancarios/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/lancamentos-bancarios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/lancamentos-bancarios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/lancamentos-bancarios/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }
}
