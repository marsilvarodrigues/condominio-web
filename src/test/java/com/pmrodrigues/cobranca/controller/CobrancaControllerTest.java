package com.pmrodrigues.cobranca.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.cobranca.dto.CancelarCobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaDTO;
import com.pmrodrigues.cobranca.dto.CobrancaResumoDTO;
import com.pmrodrigues.cobranca.dto.GerarCobrancasDTO;
import com.pmrodrigues.cobranca.dto.ResumoCobrancasDTO;
import com.pmrodrigues.cobranca.model.StatusCobranca;
import com.pmrodrigues.cobranca.service.CobrancaService;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.gateway.config.AsaasProperties;
import com.pmrodrigues.gateway.dto.AsaasWebhookPayload;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CobrancaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CobrancaService cobrancaService;
    @MockitoBean AsaasProperties asaasProperties;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(asaasProperties.apiKey()).thenReturn("valid-key");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CobrancaDTO cobrancaDTO() {
        return new CobrancaDTO(1L, 1L, "101", null, 1L, "Morador Test", "m@t.com",
                new BigDecimal("500.00"), LocalDate.now(), StatusCobranca.PENDENTE,
                "http://boleto.url", "123456", null, null, false, null, null, null);
    }

    private CobrancaResumoDTO resumoDTO() {
        return new CobrancaResumoDTO(1L, LocalDate.now(), new BigDecimal("500.00"),
                StatusCobranca.PENDENTE, null, null, false);
    }

    private ResumoCobrancasDTO resumoCobrancasDTO() {
        return new ResumoCobrancasDTO(3L, new BigDecimal("1500.00"), 1L, new BigDecimal("300.00"));
    }

    // ── gerar ─────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void gerar_asAdmin_returns201() throws Exception {
        when(cobrancaService.gerarCobrancas(any())).thenReturn(List.of(cobrancaDTO()));

        var dto = new GerarCobrancasDTO(1L, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/cobrancas/gerar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void gerar_unauthenticated_returns401() throws Exception {
        var dto = new GerarCobrancasDTO(1L, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/cobrancas/gerar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void gerar_asUser_returns403() throws Exception {
        var dto = new GerarCobrancasDTO(1L, LocalDate.now().plusDays(10));
        mockMvc.perform(post("/cobrancas/gerar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ── resumo ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void resumo_returns200() throws Exception {
        when(cobrancaService.resumo()).thenReturn(resumoCobrancasDTO());

        mockMvc.perform(get("/cobrancas/resumo").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantidadePendente").value(3));
    }

    @Test
    void resumo_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/cobrancas/resumo"))
                .andExpect(status().isUnauthorized());
    }

    // ── filterBy ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void filterBy_returns200() throws Exception {
        var page = new PageImpl<>(List.of(cobrancaDTO()), PageRequest.of(0, 20), 1);
        when(cobrancaService.filterBy(any(), any())).thenReturn(page);

        mockMvc.perform(get("/cobrancas").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_returns200() throws Exception {
        when(cobrancaService.findById(1L)).thenReturn(cobrancaDTO());

        mockMvc.perform(get("/cobrancas/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_notFound_returns404() throws Exception {
        when(cobrancaService.findById(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Cobranca not found: 99"));

        mockMvc.perform(get("/cobrancas/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── cancelar ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelar_asAdmin_returns200() throws Exception {
        when(cobrancaService.cancelar(any(), any())).thenReturn(cobrancaDTO());

        var dto = new CancelarCobrancaDTO("Motivo teste");
        mockMvc.perform(post("/cobrancas/1/cancelar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void cancelar_unauthenticated_returns401() throws Exception {
        var dto = new CancelarCobrancaDTO("Motivo");
        mockMvc.perform(post("/cobrancas/1/cancelar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void cancelar_asUser_returns403() throws Exception {
        var dto = new CancelarCobrancaDTO("Motivo");
        mockMvc.perform(post("/cobrancas/1/cancelar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // ── reenviarEmail ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void reenviarEmail_asAdmin_returns204() throws Exception {
        doNothing().when(cobrancaService).reenviarEmail(1L);

        mockMvc.perform(post("/cobrancas/1/reenviar-email")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void reenviarEmail_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/cobrancas/1/reenviar-email")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void reenviarEmail_asUser_returns403() throws Exception {
        mockMvc.perform(post("/cobrancas/1/reenviar-email")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    // ── porApartamento ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void porApartamento_returns200() throws Exception {
        var page = new PageImpl<>(List.of(resumoDTO()), PageRequest.of(0, 20), 1);
        when(cobrancaService.porApartamento(any(), any())).thenReturn(page);

        mockMvc.perform(get("/cobrancas/apartamentos/1/cobrancas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── webhook ───────────────────────────────────────────────────────────────

    @Test
    void webhook_comTokenValido_returns200() throws Exception {
        doNothing().when(cobrancaService).processarWebhook(any());

        var payload = new AsaasWebhookPayload("PAYMENT_RECEIVED",
                new AsaasWebhookPayload.AsaasWebhookPayment("pay_1", "RECEIVED", "500.00", "2026-06-08"));
        mockMvc.perform(post("/cobrancas/webhook")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .header("access_token", "valid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_comTokenInvalido_returns403() throws Exception {
        var payload = new AsaasWebhookPayload("PAYMENT_RECEIVED",
                new AsaasWebhookPayload.AsaasWebhookPayment("pay_1", "RECEIVED", "500.00", "2026-06-08"));
        mockMvc.perform(post("/cobrancas/webhook")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .header("access_token", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}
