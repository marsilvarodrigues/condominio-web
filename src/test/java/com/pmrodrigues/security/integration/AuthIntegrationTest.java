package com.pmrodrigues.security.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmrodrigues.security.model.User;
import com.pmrodrigues.security.repository.UserRepository;
import com.pmrodrigues.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final String EMAIL    = "integration@test.com";
    private static final String PASSWORD = "test-password";

    @BeforeEach
    void setUp() {
        // Physical DELETE bypasses @SQLDelete (soft-delete) to avoid unique-constraint violations on re-insert
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
        var user = new User().setEmail(EMAIL)
                .setPassword(passwordEncoder.encode(PASSWORD))
                .setName("Integration User")
                .setEnabled(true)
                .setRoles(new HashSet<>(Set.of("ROLE_USER")));
        userRepository.save(user);
    }

    @Test
    void login_withValidCredentials_returnsTokens() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.data.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@test.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("invalid_credentials"))
                .andExpect(jsonPath("$.data.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void logout_withValidToken_returns204() throws Exception {
        var accessToken = loginAndGetAccessToken();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_afterLogout_sameTokenIsRejected() throws Exception {
        var accessToken = loginAndGetAccessToken();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Second call with same (now blacklisted) token must fail
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewTokens() throws Exception {
        var loginResponse = loginAndGetResponse();
        var refreshToken  = (String) loginResponse.get("refreshToken");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_withInvalidRefreshToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.error").value("error"))
                .andExpect(jsonPath("$.data.message").value("Invalid or expired refresh token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_rotatesRefreshToken() throws Exception {
        var firstResponse  = loginAndGetResponse();
        var firstRefresh   = (String) firstResponse.get("refreshToken");

        var secondResponse = objectMapper.readValue(
                mockMvc.perform(post("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefresh))))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                Map.class);
        var secondRefresh = (String) secondResponse.get("refreshToken");

        // Old refresh token must no longer work
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isUnauthorized());

        // New one must still work
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", secondRefresh))))
                .andExpect(status().isOk());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private String loginAndGetAccessToken() throws Exception {
        return (String) loginAndGetResponse().get("accessToken");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loginAndGetResponse() throws Exception {
        var body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(body, Map.class);
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }
}
