package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.model.TipoContaBancaria;
import com.pmrodrigues.financeiro.service.ContaBancariaService;
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
class ContaBancariaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ContaBancariaService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private ContaBancariaDTO contaDto(Long id) {
        return new ContaBancariaDTO(id, 1L, "Banco do Brasil", "001",
                TipoContaBancaria.CORRENTE, "1234", "56789", "0", "Conta principal",
                null, BigDecimal.ZERO, true, LocalDateTime.now(), LocalDateTime.now());
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returns200WithPage() throws Exception {
        var page = new PageImpl<>(List.of(contaDto(1L)), PageRequest.of(0, 10), 1);
        when(service.filterBy(any(), any())).thenReturn(page);

        mockMvc.perform(get("/contas-bancarias").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/contas-bancarias").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(contaDto(1L));

        mockMvc.perform(get("/contas-bancarias/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tipo").value("CORRENTE"))
                .andExpect(jsonPath("$.data.agencia").value("1234"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "ContaBancaria not found: 99"));

        mockMvc.perform(get("/contas-bancarias/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(service.create(any())).thenReturn(contaDto(10L));

        var dto = new CreateContaBancariaDTO(1L, TipoContaBancaria.CORRENTE, "1234", "56789", "0", "Conta", null);
        mockMvc.perform(post("/contas-bancarias")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withNullBancoId_returns400() throws Exception {
        mockMvc.perform(post("/contas-bancarias")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"CORRENTE\",\"agencia\":\"1234\",\"conta\":\"56789\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankAgencia_returns400() throws Exception {
        mockMvc.perform(post("/contas-bancarias")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bancoId\":1,\"tipo\":\"CORRENTE\",\"agencia\":\"\",\"conta\":\"56789\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        var dto = new CreateContaBancariaDTO(1L, TipoContaBancaria.CORRENTE, "1234", "56789", null, null, null);
        mockMvc.perform(post("/contas-bancarias")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        var dto = new CreateContaBancariaDTO(1L, TipoContaBancaria.CORRENTE, "1234", "56789", null, null, null);
        mockMvc.perform(post("/contas-bancarias")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(contaDto(1L));

        var dto = new UpdateContaBancariaDTO("1234", "56789", "0", "Atualizado", null);
        mockMvc.perform(put("/contas-bancarias/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankAgencia_returns400() throws Exception {
        mockMvc.perform(put("/contas-bancarias/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agencia\":\"\",\"conta\":\"56789\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        var dto = new UpdateContaBancariaDTO("1234", "56789", null, null, null);
        mockMvc.perform(put("/contas-bancarias/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ── ativar / desativar ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void ativar_asAdmin_returns200() throws Exception {
        when(service.setAtiva(1L, true)).thenReturn(contaDto(1L));

        mockMvc.perform(patch("/contas-bancarias/1/ativar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativa").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void desativar_asAdmin_returns200() throws Exception {
        var inativo = new ContaBancariaDTO(1L, 1L, "Banco do Brasil", "001",
                TipoContaBancaria.CORRENTE, "1234", "56789", "0", "Conta",
                null, BigDecimal.ZERO, false, LocalDateTime.now(), LocalDateTime.now());
        when(service.setAtiva(1L, false)).thenReturn(inativo);

        mockMvc.perform(patch("/contas-bancarias/1/desativar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativa").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void ativar_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/contas-bancarias/1/ativar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/contas-bancarias/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/contas-bancarias/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/contas-bancarias/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }
}
