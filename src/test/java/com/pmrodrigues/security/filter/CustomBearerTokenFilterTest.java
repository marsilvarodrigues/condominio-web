
package com.pmrodrigues.security.filter;

import com.pmrodrigues.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomBearerTokenFilterTest {

    @Mock
    JwtDecoder jwtDecoder;

    @Mock
    TokenBlacklistService tokenBlacklistService;

    @Mock
    FilterChain filterChain;

    CustomBearerTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CustomBearerTokenFilter(jwtDecoder, tokenBlacklistService);
    }

    // ── shouldNotFilter ───────────────────────────────────────────────────

    @Test
    void shouldNotFilter_loginPath_returnsTrue() {
        var request = new MockHttpServletRequest("POST", "/auth/login");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_refreshPath_returnsTrue() {
        var request = new MockHttpServletRequest("POST", "/auth/refresh");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldNotFilter_protectedPath_returnsFalse() {
        var request = new MockHttpServletRequest("GET", "/api/resource");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ── doFilterInternal ─────────────────────────────────────────────────

    @Test
    void doFilterInternal_withoutAuthorizationHeader_passesThroughChain() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verifyNoInteractions(jwtDecoder, tokenBlacklistService);
    }

    @Test
    void doFilterInternal_withNonBearerScheme_passesThroughChain() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtDecoder, tokenBlacklistService);
    }

    @Test
    void doFilterInternal_withValidNonBlacklistedToken_passesThroughChain() throws Exception {
        when(jwtDecoder.decode("valid-token")).thenReturn(buildJwt("jti-abc"));
        when(tokenBlacklistService.isBlacklisted("jti-abc")).thenReturn(false);

        var request  = requestWithBearer("valid-token");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withBlacklistedToken_returns401AndBlocksChain() throws Exception {
        when(jwtDecoder.decode("revoked-token")).thenReturn(buildJwt("jti-revoked"));
        when(tokenBlacklistService.isBlacklisted("jti-revoked")).thenReturn(true);

        var request  = requestWithBearer("revoked-token");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("Token revoked");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_withTokenMissingJti_returns401AndBlocksChain() throws Exception {
        var jwtWithoutJti = Jwt.withTokenValue("no-jti-token")
                .header("alg", "RS256")
                .subject("user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("no-jti-token")).thenReturn(jwtWithoutJti);

        var request  = requestWithBearer("no-jti-token");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("invalid_token");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_withExpiredOrMalformedToken_returns401AndBlocksChain() throws Exception {
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("Invalid signature"));

        var request  = requestWithBearer("bad-token");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("invalid_token");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private MockHttpServletRequest requestWithBearer(String token) {
        var request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private Jwt buildJwt(String jti) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user@test.com")
                .claim("jti", jti)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}