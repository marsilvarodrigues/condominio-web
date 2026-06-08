package com.pmrodrigues.security.filter;

import com.pmrodrigues.commons.tenant.TenantContext;
import com.pmrodrigues.security.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates Bearer JWTs on every request, checks the token blacklist, and populates {@link
 * TenantContext} with the active condominio ID resolved from {@code condominio_ids} claim and the
 * {@code X-Condominio-Id} header. Skips {@code /auth/login} and {@code /auth/refresh}.
 *
 * <p>Tenant resolution rules:
 *
 * <ul>
 *   <li>Empty {@code condominio_ids} → global access (null active ID)
 *   <li>Single entry → auto-selected as active ID
 *   <li>Multiple entries → active ID taken from {@code X-Condominio-Id} header; null if header
 *       absent
 *   <li>Header value not in allowed list → 403 Forbidden
 * </ul>
 */
@Slf4j
@Component
public class CustomBearerTokenFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String CONDOMINIO_ID_HEADER = "X-Condominio-Id";

  private final JwtDecoder jwtDecoder;
  private final TokenBlacklistService tokenBlacklistService;

  public CustomBearerTokenFilter(
      JwtDecoder jwtDecoder, TokenBlacklistService tokenBlacklistService) {
    this.jwtDecoder = jwtDecoder;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  /**
   * Returns {@code true} for {@code /auth/login} and {@code /auth/refresh} so those endpoints
   * bypass JWT validation.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    boolean skip = "/auth/login".equals(path) || "/auth/refresh".equals(path);
    log.info("shouldNotFilter for path: {} - skip: {}", path, skip);
    return skip;
  }

  /**
   * Decodes the Bearer token, rejects blacklisted or jti-less tokens with 401, resolves the active
   * tenant, and delegates to the filter chain.
   *
   * @throws ServletException propagated from the downstream filter chain
   * @throws IOException propagated from the downstream filter chain
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info(
        "Processing bearer token filter for request: {} {}",
        request.getMethod(),
        request.getRequestURI());

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
        response.setHeader(
            HttpHeaders.WWW_AUTHENTICATE,
            "Bearer error=\"invalid_token\", error_description=\"Token revoked\"");
        return;
      }

      if (tokenBlacklistService.isBlacklisted(jti)) {
        log.info("Token is blacklisted for jti: {}, returning 401", jti);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(
            HttpHeaders.WWW_AUTHENTICATE,
            "Bearer error=\"invalid_token\", error_description=\"Token revoked\"");
        return;
      }

      List<Long> allowedIds = extractAllowedIds(decodedJwt);
      TenantContext.setAllowedCondominioIds(allowedIds);

      Long activeCondominioId =
          resolveActiveCondominio(allowedIds, request.getHeader(CONDOMINIO_ID_HEADER), response);
      if (activeCondominioId == null && response.getStatus() != HttpStatus.OK.value()) {
        return; // invalid header value, error status already set
      }
      TenantContext.setCondominioId(activeCondominioId);
      log.info(
          "Bearer token validated for user: {} allowedIds: {} activeCondominioId: {}",
          decodedJwt.getSubject(),
          allowedIds,
          activeCondominioId);

      try {
        filterChain.doFilter(request, response);
      } finally {
        TenantContext.clear();
      }
    } catch (JwtException ex) {
      log.error(
          "JWT validation failed for request: {} {} - error: {}",
          request.getMethod(),
          request.getRequestURI(),
          ex.getMessage());
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
    }
  }

  private List<Long> extractAllowedIds(Jwt jwt) {
    List<?> rawIds = jwt.getClaim("condominio_ids");
    if (rawIds == null) {
      return List.of();
    }
    return rawIds.stream().map(id -> ((Number) id).longValue()).toList();
  }

  /**
   * Resolves the active condominio ID from the allowed list and request header. Returns {@code
   * null} both when global access applies (empty list, no header) and on validation errors —
   * callers must check {@link HttpServletResponse#isCommitted()} to distinguish the two cases.
   */
  private Long resolveActiveCondominio(
      List<Long> allowedIds, String condominioHeader, HttpServletResponse response)
      throws IOException {
    if (condominioHeader != null && !condominioHeader.isBlank()) {
      try {
        Long requestedId = Long.parseLong(condominioHeader.trim());
        if (!allowedIds.isEmpty() && !allowedIds.contains(requestedId)) {
          log.warn("X-Condominio-Id {} not in user's allowed list {}", requestedId, allowedIds);
          response.setStatus(HttpStatus.FORBIDDEN.value());
          return null;
        }
        return requestedId;
      } catch (NumberFormatException e) {
        log.warn("Invalid X-Condominio-Id header value: {}", condominioHeader);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return null;
      }
    }
    if (allowedIds.size() == 1) {
      return allowedIds.get(0);
    }
    return null; // global access (0 IDs) or multi-condominio without header
  }
}
