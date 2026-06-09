package com.pmrodrigues.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.commons.interceptor.RequestIdInterceptor;
import com.pmrodrigues.security.dto.ChangePasswordDTO;
import com.pmrodrigues.security.dto.CreateUserDTO;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.dto.UserFilterDTO;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import com.pmrodrigues.security.service.UserService;
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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserService userService;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findAll_returnsListWith200() throws Exception {
        when(userService.filterBy(any(UserFilterDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto(1L, "a@test.com"), dto(2L, "b@test.com"))));

        mockMvc.perform(get("/users").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].email").value("a@test.com"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser
    void findAll_includesRequestIdInResponse() throws Exception {
        when(userService.filterBy(any(UserFilterDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var fixedId = UUID.randomUUID().toString();

        mockMvc.perform(get("/users").header(RequestIdInterceptor.REQUEST_ID_HEADER, fixedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request_id").value(fixedId));
    }

    @Test
    void findAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/users").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void findById_whenFound_returns200() throws Exception {
        when(userService.findUserById(1L)).thenReturn(Optional.of(dto(1L, "test@test.com")));

        mockMvc.perform(get("/users/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    @WithMockUser
    void findById_whenNotFound_returns404() throws Exception {
        when(userService.findUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/99").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/users/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── create ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_returns201WithCreatedUser() throws Exception {
        when(userService.create(any())).thenReturn(dto(1L, "new@test.com"));

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("new@test.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("new@test.com"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withDuplicateEmail_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe com o email: dup@test.com"))
                .when(userService).create(any());

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("dup@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("Usuário já existe com o email: dup@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidCondominioId_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Condomínio inválido"))
                .when(userService).create(any());

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("user@test.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("Condomínio inválido"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankEmail_returns400() throws Exception {
        var body = Map.of("email", "", "name", "Test User", "enabled", false);

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidEmailFormat_returns400() throws Exception {
        var body = Map.of("email", "not-an-email", "name", "Test User", "enabled", false);

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_returns400() throws Exception {
        var body = Map.of("email", "user@test.com", "name", "", "enabled", false);

        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.name").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_asUser_returns403() throws Exception {
        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("x@test.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/users")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto("x@test.com"))))
                .andExpect(status().isUnauthorized());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200WithUpdatedUser() throws Exception {
        var updated = dto(1L, "updated@test.com");
        when(userService.update(any())).thenReturn(updated);

        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("updated@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankEmail_returns400() throws Exception {
        var body = Map.of("email", "", "name", "Test User", "enabled", false);

        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withInvalidEmailFormat_returns400() throws Exception {
        var body = Map.of("email", "not-an-email", "name", "Test User", "enabled", false);

        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankName_returns400() throws Exception {
        var body = Map.of("email", "user@test.com", "name", "", "enabled", false);

        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.name").exists());
    }

    @Test
    @WithMockUser(username = "attacker@test.com", roles = "USER")
    void update_asUser_returns403() throws Exception {
        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "x@test.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "x@test.com", roles = "USER")
    void update_asUser_ownData_returns200() throws Exception {
        var updated = dto(1L, "x@test.com");
        when(userService.update(any())).thenReturn(updated);

        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "x@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("x@test.com"));
    }

    @Test
    void update_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/users/1")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto(1L, "x@test.com"))))
                .andExpect(status().isUnauthorized());
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_returns204() throws Exception {
        doNothing().when(userService).delete(eq(1L));

        mockMvc.perform(delete("/users/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void delete_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/users/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/users/1").header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID))
                .andExpect(status().isUnauthorized());
    }

    // ── changePassword ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void changePassword_authenticated_returns204() throws Exception {
        doNothing().when(userService).changePassword(eq(1L), any(ChangePasswordDTO.class));

        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "NewPass@123", "NewPass@123"))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void changePassword_withWrongCurrentPassword_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Senha atual inválida"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordDTO.class));

        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("WrongPass", "NewPass@123", "NewPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("Senha atual inválida"));
    }

    @Test
    @WithMockUser
    void changePassword_withMismatchedConfirmation_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nova senha não confere com a confirmação"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordDTO.class));

        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "NewPass@123", "Different@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("Nova senha não confere com a confirmação"));
    }

    @Test
    @WithMockUser
    void changePassword_withReusedPassword_returns400() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Senha já utilizada anteriormente"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordDTO.class));

        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "OldPass@1", "OldPass@1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.message").value("Senha já utilizada anteriormente"));
    }

    @Test
    @WithMockUser
    void changePassword_withBlankCurrentPassword_returns400() throws Exception {
        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("", "NewPass@123", "NewPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"));
    }

    @Test
    @WithMockUser
    void changePassword_withTooShortNewPassword_returns400() throws Exception {
        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "short", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"));
    }

    @Test
    @WithMockUser
    void changePassword_forAnotherUser_returns403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado"))
                .when(userService).changePassword(eq(2L), any(ChangePasswordDTO.class));

        mockMvc.perform(patch("/users/2/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "NewPass@123", "NewPass@123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/users/1/password")
                        .header(RequestIdInterceptor.REQUEST_ID_HEADER, REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordDTO("OldPass@1", "NewPass@123", "NewPass@123"))))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private UserDTO dto(Long id, String email) {
        return new UserDTO(id, email, "Test User", true, Set.of("ROLE_USER"), Set.of(1L), null, null);
    }

    private CreateUserDTO createDto(String email) {
        return new CreateUserDTO(email, "Test User", Set.of("ROLE_USER"), Set.of(1L));
    }
}
