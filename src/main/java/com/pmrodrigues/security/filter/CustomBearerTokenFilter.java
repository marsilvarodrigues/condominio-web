package com.pmrodrigues.security.filter;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates Bearer JWTs on every request, checks the token blacklist, and populates {@link TenantContext} with the {@code condominio_id} claim; skips {@code /auth/login} and {@code /auth/refresh}.
 */
@Slf4j
@Component
public class CustomBearerTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistService tokenBlacklistService;

    public CustomBearerTokenFilter(JwtDecoder jwtDecoder, TokenBlacklistService tokenBlacklistService) {
        this.jwtDecoder = jwtDecoder;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Returns {@code true} for {@code /auth/login} and {@code /auth/refresh} so those endpoints bypass JWT validation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = "/auth/login".equals(path) || "/auth/refresh".equals(path);
        log.info("shouldNotFilter for path: {} - skip: {}", path, skip);
        return skip;
    }

    /**
     * Decodes the Bearer token, rejects blacklisted or jti-less tokens with 401, and sets the tenant context before delegating to the filter chain.
     *
     * @throws ServletException propagated from the downstream filter chain
     * @throws IOException      propagated from the downstream filter chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("Processing bearer token filter for request: {} {}", request.getMethod(), request.getRequestURI());

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.info("No bearer token found, passing request through filter chain");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        try {
            Jwt decodedJwt = jwtDecoder.decode(token);
            String jti = decodedJwt.getClaimAsString("jti");

            if (jti == null) {
                log.info("Token missing jti claim, returning 401");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer error=\"invalid_token\", error_description=\"Token revoked\"");
                return;
            }

            if (tokenBlacklistService.isBlacklisted(jti)) {
                log.info("Token is blacklisted for jti: {}, returning 401", jti);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer error=\"invalid_token\", error_description=\"Token revoked\"");
                return;
            }

            Number condominioIdClaim = decodedJwt.getClaim("condominio_id");
            Long condominioId = condominioIdClaim != null ? condominioIdClaim.longValue() : null;
            TenantContext.setCondominioId(condominioId);
            log.info("Bearer token validated for user: {} condominioId: {}", decodedJwt.getSubject(), condominioId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } catch (JwtException ex) {
            log.error("JWT validation failed for request: {} {} - error: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        }
    }
}
