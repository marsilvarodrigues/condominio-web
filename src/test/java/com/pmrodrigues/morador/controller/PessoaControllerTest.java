package com.pmrodrigues.morador.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.morador.dto.CreatePessoaDTO;
import com.pmrodrigues.morador.dto.PessoaDTO;
import com.pmrodrigues.morador.dto.PessoaFilterDTO;
import com.pmrodrigues.morador.dto.UpdatePessoaDTO;
import com.pmrodrigues.morador.dto.HistoricoOcupacaoDTO;
import com.pmrodrigues.morador.service.HistoricoOcupacaoService;
import com.pmrodrigues.morador.service.PessoaService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PessoaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean PessoaService pessoaService;
    @MockitoBean HistoricoOcupacaoService historicoOcupacaoService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private PessoaDTO pessoaDTO() {
        return new PessoaDTO(1L, "Carlos Pereira", "MORADOR", "111.111.111-11",
                "carlos@test.com", null, null, null, 1L, null, null);
    }

    // ── filterBy ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void filterBy_returns200() throws Exception {
        Page<PessoaDTO> page = new PageImpl<>(List.of(pessoaDTO()));
        when(pessoaService.filterBy(any(PessoaFilterDTO.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/pessoas").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_returns200() throws Exception {
        when(pessoaService.findById(1L)).thenReturn(pessoaDTO());

        mockMvc.perform(get("/pessoas/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Carlos Pereira"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findById_notFound_returns404() throws Exception {
        when(pessoaService.findById(99L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Not found"));

        mockMvc.perform(get("/pessoas/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        var body = new CreatePessoaDTO("Carlos", "carlos@test.com", null, "111.111.111-11", null);
        when(pessoaService.create(any(CreatePessoaDTO.class))).thenReturn(pessoaDTO());

        mockMvc.perform(post("/pessoas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        var body = new CreatePessoaDTO("Carlos", "carlos@test.com", null, "111.111.111-11", null);

        mockMvc.perform(post("/pessoas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void create_asUser_returns403() throws Exception {
        var body = new CreatePessoaDTO("Carlos", "carlos@test.com", null, "111.111.111-11", null);

        mockMvc.perform(post("/pessoas")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        var body = new UpdatePessoaDTO("Novo Nome", null, null, null);
        when(pessoaService.update(anyLong(), any(UpdatePessoaDTO.class))).thenReturn(pessoaDTO());

        mockMvc.perform(put("/pessoas/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_returns404() throws Exception {
        when(pessoaService.update(anyLong(), any(UpdatePessoaDTO.class)))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Not found"));

        mockMvc.perform(put("/pessoas/99")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePessoaDTO(null, null, null, null))))
                .andExpect(status().isNotFound());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(pessoaService).delete(1L);

        mockMvc.perform(delete("/pessoas/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    // ── assignToApartamento ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignToApartamento_asAdmin_returns200() throws Exception {
        when(pessoaService.assignToApartamento(1L, 5L)).thenReturn(pessoaDTO());

        mockMvc.perform(post("/pessoas/1/apartamento")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .param("apartamentoId", "5"))
                .andExpect(status().isOk());
    }

    // ── removeFromApartamento ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeFromApartamento_asAdmin_returns200() throws Exception {
        when(pessoaService.removeFromApartamento(1L)).thenReturn(pessoaDTO());

        mockMvc.perform(delete("/pessoas/1/apartamento")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk());
    }

    // ── historicoOcupacao ─────────────────────────────────────────────────

    private HistoricoOcupacaoDTO historicoDTO() {
        return new HistoricoOcupacaoDTO(1L, 1L, 1L, "Carlos Pereira",
            "carlos@test.com", "111.111.111-11",
            LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 30),
            LocalDateTime.of(2024, 6, 30, 12, 0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHistoricoOcupacao_deveRetornar200_quandoAutenticadoComoAdmin() throws Exception {
        when(historicoOcupacaoService.listarPorApartamento(anyLong(), nullable(Long.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(historicoDTO())));

        mockMvc.perform(get("/pessoas/apartamentos/1/historico-ocupacao")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "PROPRIETARIO")
    void getHistoricoOcupacao_deveRetornar200_quandoAutenticadoComoProprietario() throws Exception {
        when(historicoOcupacaoService.listarPorApartamento(anyLong(), nullable(Long.class), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get("/pessoas/apartamentos/1/historico-ocupacao")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MORADOR")
    void getHistoricoOcupacao_deveRetornar403_quandoAutenticadoComoMorador() throws Exception {
        mockMvc.perform(get("/pessoas/apartamentos/1/historico-ocupacao")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHistoricoOcupacao_deveRetornar401_semAutenticacao() throws Exception {
        mockMvc.perform(get("/pessoas/apartamentos/1/historico-ocupacao"))
                .andExpect(status().isUnauthorized());
    }
}
