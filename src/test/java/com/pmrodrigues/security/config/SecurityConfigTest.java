package com.pmrodrigues.security.config;

import com.pmrodrigues.security.service.RateLimitService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    RateLimitService rateLimitService;

    @MockitoBean
    UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    // ── public endpoints ──────────────────────────────────────────────────

    @Test
    void loginEndpoint_isPublic_doesNotReturnBearerChallenge() throws Exception {
        // A public endpoint never returns WWW-Authenticate: Bearer (which signals a security filter rejection)
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"x@x.com\",\"password\":\"pw\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpoint_isPublic_doesNotReturnBearerChallenge() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"any\"}"))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    // ── protected endpoints ───────────────────────────────────────────────

    @Test
    void logoutEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndpoint_withoutToken_returnsWwwAuthenticateHeader() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, org.hamcrest.Matchers.containsString("Bearer")));
    }

    @Test
    void logoutEndpoint_withMalformedToken_returns401() throws Exception {
        doThrow(new JwtException("Invalid")).when(jwtDecoder).decode("not.a.valid.jwt");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndpoint_withValidToken_returns204() throws Exception {
        var jwt = validJwt("user@test.com", "jti-1");
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutEndpoint_withBlacklistedToken_returns401() throws Exception {
        var jwt = validJwt("user@test.com", "blacklisted-jti");
        when(jwtDecoder.decode("blacklisted-token")).thenReturn(jwt);
        when(tokenBlacklistService.isBlacklisted("blacklisted-jti")).thenReturn(true);

        // JwtDecoder lambda in JwtConfig calls isBlacklisted — mock the decoder to simulate revocation
        when(jwtDecoder.decode("blacklisted-token"))
                .thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("Token has been revoked"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer blacklisted-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndpoint_withWrongAuthorizationScheme_returns401() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // ── JWT claims → authorities ──────────────────────────────────────────

    @Test
    void logoutEndpoint_withTokenMissingRolesClaim_stillAuthenticates() throws Exception {
        // The /auth/logout endpoint has no role restriction — just requires authentication
        var jwt = Jwt.withTokenValue("no-roles-token")
                .header("alg", "RS256")
                .subject("user@test.com")
                .claim("jti", "jti-no-roles")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("no-roles-token")).thenReturn(jwt);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer no-roles-token"))
                .andExpect(status().isNoContent());
    }

    private Jwt validJwt(String subject, String jti) {
        return Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("roles", java.util.List.of("ROLE_USER"))
                .build();
    }
}
