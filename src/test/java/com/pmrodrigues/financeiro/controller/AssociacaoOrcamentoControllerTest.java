package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.model.StatusItemExtrato;
import com.pmrodrigues.financeiro.model.TipoLancamento;
import com.pmrodrigues.financeiro.service.AssociacaoOrcamentoService;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssociacaoOrcamentoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AssociacaoOrcamentoService associacaoService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private ItemExtratoComOrcamentoResponse extratoResponse(Long id, ItemOrcamentoResumoResponse orcamento) {
        return new ItemExtratoComOrcamentoResponse(
                id, LocalDate.now(), BigDecimal.valueOf(500),
                TipoLancamento.CREDITO, "Taxa cond.", StatusItemExtrato.PENDENTE, orcamento);
    }

    private ItemOrcamentoResumoResponse orcamentoResumo() {
        return new ItemOrcamentoResumoResponse(10L, "1.1", "Taxa cond.", BigDecimal.valueOf(500), BigDecimal.ZERO);
    }

    // ── POST /conciliacao/associacao/{id} ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void associar_asAdmin_returns200WithData() throws Exception {
        when(associacaoService.associar(eq(20L), eq(10L))).thenReturn(extratoResponse(20L, orcamentoResumo()));

        mockMvc.perform(post("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemOrcamentoId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.itemOrcamento.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void associar_withNullItemOrcamentoId_returns400() throws Exception {
        mockMvc.perform(post("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemOrcamentoId\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void associar_whenExtratoNotFound_returns404() throws Exception {
        when(associacaoService.associar(eq(99L), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "ItemExtrato not found: 99"));

        mockMvc.perform(post("/conciliacao/associacao/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemOrcamentoId\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void associar_asUser_returns403() throws Exception {
        mockMvc.perform(post("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemOrcamentoId\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void associar_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemOrcamentoId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /conciliacao/associacao/{id} ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void desassociar_asAdmin_returns200WithNullOrcamento() throws Exception {
        when(associacaoService.desassociar(eq(20L), any())).thenReturn(extratoResponse(20L, null));

        mockMvc.perform(delete("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Erro de classificação\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.itemOrcamento").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void desassociar_withBlankJustificativa_returns400() throws Exception {
        mockMvc.perform(delete("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void desassociar_whenExtratoNotFound_returns404() throws Exception {
        when(associacaoService.desassociar(eq(99L), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "ItemExtrato not found: 99"));

        mockMvc.perform(delete("/conciliacao/associacao/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Justificativa\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void desassociar_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Justificativa\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void desassociar_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/conciliacao/associacao/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Justificativa\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /conciliacao/associacao/sugestoes/{id} ────────────────────────

    @Test
    @WithMockUser
    void sugestoes_returns200WithList() throws Exception {
        var sugestao = new SugestaoItemOrcamentoResponse(
                10L, "1.1", "Taxa condominial", BigDecimal.valueOf(500), BigDecimal.ZERO,
                80, true, "tipo compatível, palavras-chave em comum");
        when(associacaoService.sugerirItemOrcamento(20L)).thenReturn(List.of(sugestao));

        mockMvc.perform(get("/conciliacao/associacao/sugestoes/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].score").value(80))
                .andExpect(jsonPath("$.data[0].tipoCompativel").value(true));
    }

    @Test
    @WithMockUser
    void sugestoes_whenExtratoNotFound_returns404() throws Exception {
        when(associacaoService.sugerirItemOrcamento(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "ItemExtrato not found: 99"));

        mockMvc.perform(get("/conciliacao/associacao/sugestoes/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void sugestoes_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/conciliacao/associacao/sugestoes/20")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /conciliacao/associacao/contribuicao/{id} ─────────────────────

    @Test
    @WithMockUser
    void contribuicao_returns200WithData() throws Exception {
        var response = new ContribuicaoResponse(
                10L, BigDecimal.valueOf(1000), BigDecimal.valueOf(500),
                new java.math.BigDecimal("50.00"), 2);
        when(associacaoService.calcularContribuicao(10L)).thenReturn(response);

        mockMvc.perform(get("/conciliacao/associacao/contribuicao/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemOrcamentoId").value(10))
                .andExpect(jsonPath("$.data.totalItensAssociados").value(2))
                .andExpect(jsonPath("$.data.percentualRealizado").value(50.00));
    }

    @Test
    @WithMockUser
    void contribuicao_whenOrcamentoNotFound_returns404() throws Exception {
        when(associacaoService.calcularContribuicao(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "ItemOrcamento not found: 99"));

        mockMvc.perform(get("/conciliacao/associacao/contribuicao/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void contribuicao_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/conciliacao/associacao/contribuicao/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }
}
