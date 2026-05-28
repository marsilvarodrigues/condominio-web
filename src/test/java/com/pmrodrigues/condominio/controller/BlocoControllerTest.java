package com.pmrodrigues.condominio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.condominio.dto.BlocoDTO;
import com.pmrodrigues.condominio.dto.BlocoFilterDTO;
import com.pmrodrigues.condominio.dto.CreateBlocoDTO;
import com.pmrodrigues.condominio.service.BlocoService;
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
class BlocoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean BlocoService blocoService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returnsListWith200() throws Exception {
        when(blocoService.filterBy(any(BlocoFilterDTO.class)))
                .thenReturn(List.of(blocoDto(1L, 1, "A"), blocoDto(2L, 2, "B")));

        mockMvc.perform(get("/blocos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void findAll_withBlocoFilter_delegatesToFilterBy() throws Exception {
        when(blocoService.filterBy(any(BlocoFilterDTO.class)))
                .thenReturn(List.of(blocoDto(1L, 1, "A")));

        mockMvc.perform(get("/blocos").param("bloco", "A")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].bloco").value("A"));
    }

    @Test
    @WithMockUser
    void findAll_withNoFilter_delegatesToFilterByWithNull() throws Exception {
        when(blocoService.filterBy(any(BlocoFilterDTO.class))).thenReturn(List.of());

        mockMvc.perform(get("/blocos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/blocos").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(blocoService.findById(1L)).thenReturn(Optional.of(blocoDto(1L, 1, "A")));

        mockMvc.perform(get("/blocos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.bloco").value("A"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(blocoService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/blocos/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/blocos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201() throws Exception {
        when(blocoService.create(any())).thenReturn(blocoDto(1L, 1, "A"));

        mockMvc.perform(post("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlocoDTO(1, "A"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bloco").value("A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankBloco_returns400() throws Exception {
        var body = Map.of("numero", 1, "bloco", "");

        mockMvc.perform(post("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.bloco").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlocoDTO(1, "A"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/blocos")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlocoDTO(1, "A"))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        when(blocoService.update(any())).thenReturn(blocoDto(1L, 2, "B"));

        mockMvc.perform(put("/blocos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blocoDto(1L, 2, "B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bloco").value("B"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankBloco_returns400() throws Exception {
        var body = Map.of("numero", 1, "bloco", "");

        mockMvc.perform(put("/blocos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.bloco").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/blocos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blocoDto(1L, 1, "A"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/blocos/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blocoDto(1L, 1, "A"))))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(blocoService).delete(1L);

        mockMvc.perform(delete("/blocos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/blocos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/blocos/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── helper ────────────────────────────────────────────────────────────

    private BlocoDTO blocoDto(Long id, Integer numero, String bloco) {
        return new BlocoDTO(id, numero, bloco, null, null);
    }
}
