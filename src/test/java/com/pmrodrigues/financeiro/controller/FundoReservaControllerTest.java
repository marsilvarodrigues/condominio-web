package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.model.TipoMovimentacao;
import com.pmrodrigues.financeiro.service.FundoReservaService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FundoReservaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean FundoReservaService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private FundoReservaDTO fundoDto() {
        return new FundoReservaDTO(1L, new BigDecimal("10.00"), new BigDecimal("5000.00"),
                null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    private FundoReservaMovimentacaoDTO movDto(TipoMovimentacao tipo) {
        return new FundoReservaMovimentacaoDTO(10L, tipo, new BigDecimal("1000.00"),
                "Justificativa", LocalDate.now(), LocalDateTime.now());
    }

    // ── get ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void get_returns200() throws Exception {
        when(service.get()).thenReturn(fundoDto());

        mockMvc.perform(get("/fundo-reserva").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void get_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/fundo-reserva").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(service.create(any())).thenReturn(fundoDto());

        mockMvc.perform(post("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFundoReservaDTO(new BigDecimal("10.00"), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenAlreadyExists_returns409() throws Exception {
        when(service.create(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Fundo already exists"));

        mockMvc.perform(post("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFundoReservaDTO(new BigDecimal("10.00"), null))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidPercentual_returns400() throws Exception {
        mockMvc.perform(post("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percentualArrecadacao\":150.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFundoReservaDTO(new BigDecimal("10.00"), null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateFundoReservaDTO(new BigDecimal("10.00"), null))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(service.update(any())).thenReturn(fundoDto());

        mockMvc.perform(put("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateFundoReservaDTO(new BigDecimal("15.00"), null))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/fundo-reserva")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateFundoReservaDTO(new BigDecimal("15.00"), null))))
                .andExpect(status().isForbidden());
    }

    // ── creditar ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void creditar_asAdmin_returns201() throws Exception {
        when(service.creditar(any())).thenReturn(movDto(TipoMovimentacao.CREDITO));

        mockMvc.perform(post("/fundo-reserva/creditar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreditarFundoDTO(new BigDecimal("1000.00"), "Depósito mensal", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipo").value("CREDITO"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void creditar_asUser_returns403() throws Exception {
        mockMvc.perform(post("/fundo-reserva/creditar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreditarFundoDTO(new BigDecimal("100.00"), null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void creditar_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/fundo-reserva/creditar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreditarFundoDTO(new BigDecimal("100.00"), null, null))))
                .andExpect(status().isUnauthorized());
    }

    // ── debitar ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void debitar_asAdmin_returns201() throws Exception {
        when(service.debitar(any())).thenReturn(movDto(TipoMovimentacao.DEBITO));

        mockMvc.perform(post("/fundo-reserva/debitar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DebitarFundoDTO(new BigDecimal("500.00"), "Manutenção", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipo").value("DEBITO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void debitar_whenInsufficientBalance_returns409() throws Exception {
        when(service.debitar(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Saldo insuficiente"));

        mockMvc.perform(post("/fundo-reserva/debitar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DebitarFundoDTO(new BigDecimal("99999.00"), "x", null))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "USER")
    void debitar_asUser_returns403() throws Exception {
        mockMvc.perform(post("/fundo-reserva/debitar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DebitarFundoDTO(new BigDecimal("100.00"), "x", null))))
                .andExpect(status().isForbidden());
    }

    // ── listMovimentacoes ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void listMovimentacoes_returns200() throws Exception {
        when(service.listMovimentacoes(any())).thenReturn(
                new PageImpl<>(List.of(movDto(TipoMovimentacao.CREDITO)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/fundo-reserva/movimentacoes")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listMovimentacoes_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/fundo-reserva/movimentacoes")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }
}
