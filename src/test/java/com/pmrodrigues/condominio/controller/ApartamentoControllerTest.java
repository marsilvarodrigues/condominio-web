package com.pmrodrigues.condominio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.condominio.dto.ApartamentoDTO;
import com.pmrodrigues.condominio.dto.ApartamentoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateApartamentoDTO;
import com.pmrodrigues.condominio.service.ApartamentoService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
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
class ApartamentoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ApartamentoService apartamentoService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returnsListWith200() throws Exception {
        when(apartamentoService.filterBy(any(ApartamentoFilterDTO.class)))
                .thenReturn(List.of(aptDto(1L, "101"), aptDto(2L, "102")));

        mockMvc.perform(get("/apartamentos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void findAll_withBlocoIdFilter_delegatesToFilterBy() throws Exception {
        when(apartamentoService.filterBy(any(ApartamentoFilterDTO.class)))
                .thenReturn(List.of(aptDto(1L, "101")));

        mockMvc.perform(get("/apartamentos").param("blocoId", "5")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].numero").value("101"));
    }

    @Test
    @WithMockUser
    void findAll_withNumeroFilter_delegatesToFilterBy() throws Exception {
        when(apartamentoService.filterBy(any(ApartamentoFilterDTO.class)))
                .thenReturn(List.of(aptDto(1L, "101"), aptDto(2L, "102")));

        mockMvc.perform(get("/apartamentos").param("numero", "10")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void findAll_withBothFilters_delegatesToFilterBy() throws Exception {
        when(apartamentoService.filterBy(any(ApartamentoFilterDTO.class)))
                .thenReturn(List.of(aptDto(1L, "101")));

        mockMvc.perform(get("/apartamentos").param("blocoId", "5").param("numero", "101")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/apartamentos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(apartamentoService.findById(1L)).thenReturn(Optional.of(aptDto(1L, "101")));

        mockMvc.perform(get("/apartamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.numero").value("101"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(apartamentoService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/apartamentos/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/apartamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(apartamentoService.create(any())).thenReturn(aptDto(1L, "101"));

        mockMvc.perform(post("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApartamentoDTO(1L, "101"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value("101"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankNumero_returns400() throws Exception {
        var body = Map.of("blocoId", 1, "numero", "");

        mockMvc.perform(post("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.numero").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApartamentoDTO(1L, "101"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/apartamentos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateApartamentoDTO(1L, "101"))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(apartamentoService.update(any())).thenReturn(aptDto(1L, "102"));

        mockMvc.perform(put("/apartamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aptDto(1L, "102"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.numero").value("102"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankNumero_returns400() throws Exception {
        var body = Map.of("numero", "");

        mockMvc.perform(put("/apartamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.numero").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/apartamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aptDto(1L, "101"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/apartamentos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aptDto(1L, "101"))))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(apartamentoService).delete(1L);

        mockMvc.perform(delete("/apartamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/apartamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/apartamentos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── helper ────────────────────────────────────────────────────────────

    private ApartamentoDTO aptDto(Long id, String numero) {
        return new ApartamentoDTO(id, null, numero, null, null);
    }
}
