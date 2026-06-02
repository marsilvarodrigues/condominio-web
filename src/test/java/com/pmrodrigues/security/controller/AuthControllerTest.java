package com.pmrodrigues.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.security.dto.UserDTO;
import com.pmrodrigues.security.service.JwtService;
import com.pmrodrigues.security.service.RateLimitService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import com.pmrodrigues.security.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    RateLimitService rateLimitService;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    UserService userService;

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_returns200WithTokens() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("user@test.com", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("user@test.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.data.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_withBlankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_withInvalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("not-an-email", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.email").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_withBlankPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("user@test.com", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.password").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_storesRefreshToken() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("user@test.com", "password"))))
                .andExpect(status().isOk());

        verify(tokenBlacklistService).storeRefreshToken(eq("user@test.com"), eq("refresh-token"), any());
    }

    // ── logout ────────────────────────────────────────────────────────────

    @Test
    void logout_withValidToken_returns204() throws Exception {
        var jwt = buildJwt("user@test.com", "jti-abc");
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(tokenBlacklistService).blacklistToken(eq("jti-abc"), any());
        verify(tokenBlacklistService).deleteRefreshToken("user@test.com");
    }

    @Test
    void logout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withInvalidToken_returns401() throws Exception {
        when(jwtDecoder.decode("bad-token"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("Invalid"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Test
    void refresh_withValidRefreshToken_returns200WithNewTokens() throws Exception {
        var userDetails = User.withUsername("user@test.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        when(tokenBlacklistService.getEmailByRefreshToken("valid-refresh")).thenReturn(Optional.of("user@test.com"));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("valid-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refresh_withExpiredRefreshToken_returns401() throws Exception {
        when(tokenBlacklistService.getEmailByRefreshToken("expired-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("expired-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("error"))
                .andExpect(jsonPath("$.data.message").value("Invalid or expired refresh token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_withBlankRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.refreshToken").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_rotatesRefreshToken() throws Exception {
        var userDetails = User.withUsername("user@test.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        when(tokenBlacklistService.getEmailByRefreshToken("old-refresh")).thenReturn(Optional.of("user@test.com"));
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("old-refresh"))))
                .andExpect(status().isOk());

        verify(tokenBlacklistService).deleteRefreshToken("user@test.com");
        verify(tokenBlacklistService).storeRefreshToken(eq("user@test.com"), eq("new-refresh"), any());
    }

    // ── activate ─────────────────────────────────────────────────────────

    @Test
    void activate_withValidToken_returns200WithTokensAndRedirect() throws Exception {
        var dto = new UserDTO(42L, "user@test.com", "Maria Santos", true, Set.of("ROLE_USER"), Set.of(1L), null, null);
        var userDetails = User.withUsername("user@test.com").password("pass").authorities("ROLE_USER").build();

        when(userService.activateAccount("valid-token")).thenReturn(dto);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateRequest("valid-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.redirectUrl").value("/change-password"));
    }

    @Test
    void activate_withInvalidToken_returns404() throws Exception {
        when(userService.activateAccount("bad-token"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Invalid activation token"));

        mockMvc.perform(post("/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateRequest("bad-token"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void activate_withExpiredToken_returns400() throws Exception {
        when(userService.activateAccount("expired-token"))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Activation token has expired"));

        mockMvc.perform(post("/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateRequest("expired-token"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activate_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.error").value("validation_error"))
                .andExpect(jsonPath("$.data.fields.activationToken").exists());
    }

    @Test
    void activate_storesRefreshToken() throws Exception {
        var dto = new UserDTO(1L, "user@test.com", "Test User", true, Set.of("ROLE_USER"), Set.of(1L), null, null);
        var userDetails = User.withUsername("user@test.com").password("pass").authorities("ROLE_USER").build();

        when(userService.activateAccount("valid-token")).thenReturn(dto);
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateRequest("valid-token"))))
                .andExpect(status().isOk());

        verify(tokenBlacklistService).storeRefreshToken(eq("user@test.com"), eq("refresh-token"), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Jwt buildJwt(String subject, String jti) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    record AuthRequest(String email, String password) {}

    record RefreshRequest(String refreshToken) {}

    record ActivateRequest(String activationToken) {}
}
