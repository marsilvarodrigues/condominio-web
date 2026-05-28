package com.pmrodrigues.financeiro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.financeiro.dto.*;
import com.pmrodrigues.financeiro.model.StatusOrcamento;
import com.pmrodrigues.financeiro.model.TipoConta;
import com.pmrodrigues.financeiro.service.OrcamentoAnualService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrcamentoAnualControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OrcamentoAnualService service;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private OrcamentoAnualDTO orcamentoDto(Long id, StatusOrcamento status) {
        return new OrcamentoAnualDTO(id, 2026, status, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ItemOrcamentoDTO itemDto(Long id) {
        return new ItemOrcamentoDTO(id, 5L, "Limpeza", TipoConta.DESPESA,
                new BigDecimal("500.00"), BigDecimal.ZERO);
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returns200() throws Exception {
        when(service.filterBy(any())).thenReturn(List.of(orcamentoDto(1L, StatusOrcamento.RASCUNHO)));

        mockMvc.perform(get("/orcamentos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/orcamentos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(orcamentoDto(1L, StatusOrcamento.RASCUNHO)));

        mockMvc.perform(get("/orcamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exercicio").value(2026));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orcamentos/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(service.create(any())).thenReturn(orcamentoDto(1L, StatusOrcamento.RASCUNHO));

        mockMvc.perform(post("/orcamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2026))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidExercicio_returns400() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exercicio\":1900}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2026))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2026))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(service.update(eq(1L), any())).thenReturn(orcamentoDto(1L, StatusOrcamento.RASCUNHO));

        mockMvc.perform(put("/orcamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2027))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_whenApproved_returns409() throws Exception {
        when(service.update(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot update approved"));

        mockMvc.perform(put("/orcamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2026))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/orcamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrcamentoAnualDTO(2026))))
                .andExpect(status().isForbidden());
    }

    // ── aprovar ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void aprovar_asAdmin_returns200() throws Exception {
        when(service.aprovar(eq(1L), any())).thenReturn(orcamentoDto(1L, StatusOrcamento.APROVADO));

        mockMvc.perform(patch("/orcamentos/1/aprovar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AprovarOrcamentoDTO(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APROVADO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void aprovar_whenAlreadyApproved_returns409() throws Exception {
        when(service.aprovar(eq(1L), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Already approved for year"));

        mockMvc.perform(patch("/orcamentos/1/aprovar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AprovarOrcamentoDTO(10))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "USER")
    void aprovar_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/orcamentos/1/aprovar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AprovarOrcamentoDTO(10))))
                .andExpect(status().isForbidden());
    }

    // ── encerrar ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void encerrar_asAdmin_returns200() throws Exception {
        when(service.encerrar(1L)).thenReturn(orcamentoDto(1L, StatusOrcamento.ENCERRADO));

        mockMvc.perform(patch("/orcamentos/1/encerrar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENCERRADO"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void encerrar_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/orcamentos/1/encerrar")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isForbidden());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/orcamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/orcamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    // ── addItem ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void addItem_asAdmin_returns201() throws Exception {
        when(service.addItem(eq(1L), any())).thenReturn(itemDto(10L));

        mockMvc.perform(post("/orcamentos/1/itens")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateItemOrcamentoDTO(5L, new BigDecimal("500.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addItem_withNullPlanoContas_returns400() throws Exception {
        mockMvc.perform(post("/orcamentos/1/itens")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorPrevisto\":500.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void addItem_asUser_returns403() throws Exception {
        mockMvc.perform(post("/orcamentos/1/itens")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateItemOrcamentoDTO(5L, new BigDecimal("500.00")))))
                .andExpect(status().isForbidden());
    }

    // ── updateItem ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateItem_asAdmin_returns200() throws Exception {
        when(service.updateItem(eq(1L), eq(10L), any())).thenReturn(itemDto(10L));

        mockMvc.perform(put("/orcamentos/1/itens/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateItemOrcamentoDTO(5L, new BigDecimal("600.00")))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateItem_asUser_returns403() throws Exception {
        mockMvc.perform(put("/orcamentos/1/itens/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateItemOrcamentoDTO(5L, new BigDecimal("600.00")))))
                .andExpect(status().isForbidden());
    }

    // ── deleteItem ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteItem_asAdmin_returns204() throws Exception {
        doNothing().when(service).deleteItem(1L, 10L);

        mockMvc.perform(delete("/orcamentos/1/itens/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteItem_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/orcamentos/1/itens/10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }
}
