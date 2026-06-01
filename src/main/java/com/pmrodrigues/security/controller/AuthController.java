package com.pmrodrigues.security.controller;

import com.pmrodrigues.security.config.JwtProperties;
import com.pmrodrigues.security.dto.ActivateAccountDTO;
import com.pmrodrigues.security.dto.ActivationResponseDTO;
import com.pmrodrigues.security.dto.AuthRequestDTO;
import com.pmrodrigues.security.dto.AuthResponseDTO;
import com.pmrodrigues.security.dto.RefreshRequestDTO;
import com.pmrodrigues.security.service.JwtService;
import com.pmrodrigues.security.service.RateLimitService;
import com.pmrodrigues.security.service.TokenBlacklistService;
import com.pmrodrigues.security.service.UserDetailsServiceImpl;
import com.pmrodrigues.security.service.UserService;
import com.pmrodrigues.commons.versioning.ApiVersion;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * REST controller exposing custom login, logout, and token-refresh endpoints outside of the standard OAuth2 flows.
 */
@Slf4j
@ApiVersion("1")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final RateLimitService rateLimitService;

    /**
     * Authenticates the user and returns a new access token and refresh token.
     *
     * @param request credentials containing the user's email and password
     * @return {@code 200} with {@link AuthResponseDTO} containing both tokens
     */
    @PostMapping("/login")
    @Timed(value = "auth.controller.login", description = "Login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request, HttpServletRequest httpRequest) {
        rateLimitService.checkLoginRateLimit(extractClientIp(httpRequest));
        log.info("Login request received for user: {}", request.email());

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var accessToken = jwtService.generateAccessToken(authentication);
        var refreshToken = jwtService.generateRefreshToken();

        tokenBlacklistService.storeRefreshToken(
                authentication.getName(),
                refreshToken,
                Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration())
        );

        log.info("Login successful for user: {}", authentication.getName());
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .build());
    }

    /**
     * Invalidates the caller's current JWT and removes the associated refresh token from the store.
     *
     * @param jwt the currently authenticated JWT principal
     * @return {@code 204 No Content}
     */
    @PostMapping("/logout")
    @Timed(value = "auth.controller.logout", description = "Logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        log.info("Logout request received for user: {}", jwt.getSubject());

        var remaining = Duration.between(Instant.now(), jwt.getExpiresAt());
        tokenBlacklistService.blacklistToken(jwt.getId(), remaining);
        tokenBlacklistService.deleteRefreshToken(jwt.getSubject());

        log.info("Logout successful for user: {}", jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a user account using the one-time token sent in the activation email.
     * The account is enabled and the user receives short-lived credentials; the response
     * contains a {@code redirectUrl} pointing to the change-password screen.
     *
     * @param request the activation token from the email link
     * @return {@code 200} with {@link ActivationResponseDTO} containing tokens and a redirect URL
     */
    @PostMapping("/activate")
    @Timed(value = "auth.controller.activate", description = "Activate account")
    public ResponseEntity<ActivationResponseDTO> activate(@Valid @RequestBody ActivateAccountDTO request) {
        log.info("Account activation request received");

        var userDTO = userService.activateAccount(request.activationToken());

        var userDetails = userDetailsService.loadUserByUsername(userDTO.email());
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        var accessToken = jwtService.generateAccessToken(authentication);
        var refreshToken = jwtService.generateRefreshToken();

        tokenBlacklistService.storeRefreshToken(
                userDTO.email(),
                refreshToken,
                Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration())
        );

        log.info("Account activated successfully for user id: {}", userDTO.id());
        return ResponseEntity.ok(new ActivationResponseDTO(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenExpiration(),
                userDTO.id(),
                "/change-password"
        ));
    }

    /**
     * Issues a new access token and rotates the refresh token, invalidating the supplied one.
     *
     * @param request the refresh token obtained at login or a previous refresh
     * @return {@code 200} with a fresh {@link AuthResponseDTO}
     * @throws org.springframework.web.server.ResponseStatusException {@code 401} if the refresh token is invalid or expired
     */
    @PostMapping("/refresh")
    @Timed(value = "auth.controller.refresh", description = "Refresh token")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request, HttpServletRequest httpRequest) {
        rateLimitService.checkRefreshRateLimit(extractClientIp(httpRequest));
        log.info("Token refresh request received");

        var email = tokenBlacklistService.getEmailByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired refresh token"));

        var userDetails = userDetailsService.loadUserByUsername(email);
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        tokenBlacklistService.deleteRefreshToken(email);
        var newRefreshToken = jwtService.generateRefreshToken();
        tokenBlacklistService.storeRefreshToken(
                email,
                newRefreshToken,
                Duration.ofSeconds(jwtProperties.getRefreshTokenExpiration())
        );

        log.info("Token refreshed successfully for user: {}", email);
        return ResponseEntity.ok(AuthResponseDTO.builder()
                .accessToken(jwtService.generateAccessToken(authentication))
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .build());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}